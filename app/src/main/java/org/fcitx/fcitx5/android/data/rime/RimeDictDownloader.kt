/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.rime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.fcitx.fcitx5.android.utils.appContext
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** What the downloader is doing right now, so the settings page can show it. */
sealed interface RimeDictState {
    data object Idle : RimeDictState
    data class Resolving(val what: String) : RimeDictState
    data class Downloading(
        val what: String,
        val downloaded: Long,
        val total: Long,
    ) : RimeDictState {
        val percent: Int
            get() = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
    }

    data class Extracting(val what: String) : RimeDictState
    data class Failed(val what: String, val message: String) : RimeDictState
    data class Done(val what: String, val detail: String) : RimeDictState
}

/**
 * Downloads and unpacks the 万象拼音 schema package (and optionally the language model) into Rime's
 * **user directory**, which is the writable half of librime's data path and therefore the only place
 * an app can install a schema without touching the deployed assets.
 *
 * Everything is plain `HttpURLConnection`/`ZipInputStream`: the app has no HTTP client dependency
 * and these files are a few tens of megabytes at most.
 */
object RimeDictDownloader {

    private val _state = MutableStateFlow<RimeDictState>(RimeDictState.Idle)
    val state: StateFlow<RimeDictState> = _state

    /** Set while a job runs so two callers (auto + manual) cannot download at the same time. */
    @Volatile
    private var running = false

    val isRunning: Boolean get() = running

    /**
     * Installs [variant] (with [scheme] for Pro) and, when [withGrammar] is set, the language model.
     * Returns true when something was installed.
     */
    suspend fun install(
        variant: RimeDictPackages.Variant = RimeDictPackages.Variant.Base,
        scheme: String = "",
        withGrammar: Boolean = false,
        force: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        if (running) {
            Timber.i("Rime dictionary install already running")
            return@withContext false
        }
        running = true
        val prefs = RimePrefs.ensureInitialized(appContext)
        try {
            val target = RimeManager.userDir.also { it.mkdirs() }
            val tag = prefs.dictTag.getValue().ifBlank { resolveTag() }
            if (tag.isBlank()) {
                _state.value = RimeDictState.Failed("", "无法获取版本号")
                return@withContext false
            }
            prefs.dictTag.setValue(tag)

            val label = variant.label + if (variant == RimeDictPackages.Variant.Pro) " · $scheme" else ""
            val asset = RimeDictPackages.assetName(variant, scheme)
            val installedTag = prefs.dictInstalledTag.getValue()
            val installedVariant = prefs.dictVariant.getValue()

            val needsPackage = force || installedTag != tag || installedVariant != variant.id
            if (needsPackage) {
                val stream = fetch(RimeDictPackages.packageUrl(tag, variant, scheme), label)
                    ?: return@withContext false
                unzipInto(stream, target, label)
                stream.close()
            }

            if (withGrammar) {
                val gram = File(target, RimeDictPackages.GRAM_FILE)
                if (force || !gram.isFile || gram.length() < RimeDictPackages.GRAM_SIZE_HINT / 2) {
                    val stream = fetch(RimeDictPackages.gramUrl(), "语法模型") ?: return@withContext false
                    _state.value = RimeDictState.Extracting("语法模型")
                    gram.outputStream().use { out -> stream.use { it.copyTo(out) } }
                }
            }

            prefs.dictInstalledTag.setValue(tag)
            prefs.dictVariant.setValue(variant.id)
            prefs.dictScheme.setValue(scheme)
            prefs.dictGrammar.setValue(withGrammar)
            // A downloaded schema only becomes usable after librime recompiles it.
            RimeManager.applyConfiguration(appContext, force = true)
            _state.value = RimeDictState.Done(label, tag)
            true
        } catch (t: Throwable) {
            Timber.w(t, "Rime dictionary install failed")
            _state.value = RimeDictState.Failed("", t.localizedMessage ?: t.toString())
            false
        } finally {
            running = false
        }
    }

    fun resetState() {
        if (!running) _state.value = RimeDictState.Idle
    }

    /** Asks GitHub for the newest 万象拼音 tag, via the mirrors in order. */
    private fun resolveTag(): String {
        _state.value = RimeDictState.Resolving("版本")
        val api = "https://api.github.com/repos/${RimeDictPackages.REPO}/releases/latest"
        for (mirror in RimeDictPackages.MIRRORS) {
            val tag = runCatching {
                open("${mirror}$api").use { connection ->
                    if (connection.responseCode != 200) return@use null
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
                }
            }.getOrNull()
            if (!tag.isNullOrBlank()) return tag
        }
        Timber.w("Could not resolve the newest rime-wanxiang tag; using %s", RimeDictPackages.FALLBACK_TAG)
        return RimeDictPackages.FALLBACK_TAG
    }

    /** Opens [url], trying the mirrors in order; the caller closes the returned stream. */
    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "fcitx5-android")
        }

    private fun fetch(url: String, what: String): InputStream? {
        for (mirror in RimeDictPackages.MIRRORS) {
            val candidate = "$mirror$url"
            val connection = runCatching { open(candidate) }.getOrNull() ?: continue
            val code = runCatching { connection.responseCode }.getOrDefault(-1)
            if (code != 200) {
                Timber.w("Download %s answered %s", candidate, code)
                runCatching { connection.disconnect() }
                continue
            }
            val total = connection.contentLengthLong
            val raw = BufferedInputStream(connection.inputStream, 64 * 1024)
            return object : InputStream() {
                private var read = 0L

                override fun read(): Int {
                    val b = raw.read()
                    if (b >= 0) report(++read, total, what)
                    return b
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val n = raw.read(b, off, len)
                    if (n > 0) {
                        read += n
                        report(read, total, what)
                    }
                    return n
                }

                override fun close() {
                    runCatching { raw.close() }
                    runCatching { connection.disconnect() }
                }
            }
        }
        _state.value = RimeDictState.Failed(what, "下载失败（所有镜像均不可用）")
        return null
    }

    private fun report(downloaded: Long, total: Long, what: String) {
        val current = _state.value
        // Throttle: only publish every ~256 KiB so recomposition stays cheap.
        if (current is RimeDictState.Downloading &&
            current.what == what && downloaded - current.downloaded < 256 * 1024
        ) {
            return
        }
        _state.value = RimeDictState.Downloading(what, downloaded, total)
    }

    private fun unzipInto(stream: InputStream, target: File, what: String) {
        _state.value = RimeDictState.Extracting(what)
        val root = target.canonicalFile
        ZipInputStream(BufferedInputStream(stream, 64 * 1024)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val out = File(target, entry.name)
                // Zip-slip guard: never write outside the Rime user directory.
                if (!out.canonicalPath.startsWith(root.path + File.separator)) {
                    Timber.w("Skipping suspicious zip entry %s", entry.name)
                    zip.closeEntry()
                    continue
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
            }
        }
    }
}
