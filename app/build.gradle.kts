plugins {
    id("org.fcitx.fcitx5.android.app-convention")
    id("org.fcitx.fcitx5.android.native-app-convention")
    id("org.fcitx.fcitx5.android.build-metadata")
    id("org.fcitx.fcitx5.android.data-descriptor")
    id("org.fcitx.fcitx5.android.fcitx-component")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

/**
 * Java/Kotlin package and JNI symbol prefix.
 *
 * This deliberately stays `org.fcitx.fcitx5.android`: it is the code namespace, it is baked into
 * the native `Java_org_fcitx_fcitx5_android_*` JNI entry points, and it is what the original
 * fcitx5-android plugin packages interoperate with. Only the install identity changes below.
 */
val packageBase = "org.fcitx.fcitx5.android"

/** Install identity (what users see as the app's package name). */
val appIdBase = "org.fcitx5.xuexgao"

/** fx is the canonical variant, so it carries no suffix. */
val appIdFxSuffix = ""

/** The optional mainline variant gets its own identity so both can be installed side by side. */
val appIdMainlineSuffix = ".mainline"
val flavorFx = "fx"
val flavorMainline = "mainline"
val appLabelDefault = "@string/app_name"
val appLabelMainlineRelease = "@string/app_name_mainline_release"
val appLabelMainlineDebug = "@string/app_name_mainline_debug"
val originalPluginManifestAction = "$packageBase.plugin.MANIFEST"
val originalDebugPluginManifestAction = "$packageBase.debug.plugin.MANIFEST"
val originalIpcAction = "$packageBase.IPC"
val originalDebugIpcAction = "$packageBase.debug.IPC"
val imeSettingsActivity = "$packageBase.ui.main.MainActivity"
val includeMainlineFlavor =
    providers.gradleProperty("includeMainlineFlavor").map(String::toBoolean).orElse(false)

android {
    namespace = packageBase

    defaultConfig {
        applicationId = appIdBase
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = appLabelDefault
        manifestPlaceholders["originalPluginManifestAction"] = originalPluginManifestAction
        manifestPlaceholders["originalDebugPluginManifestAction"] = originalDebugPluginManifestAction
        manifestPlaceholders["originalIpcAction"] = originalIpcAction
        manifestPlaceholders["originalDebugIpcAction"] = originalDebugIpcAction
        buildConfigField("String", "ORIGINAL_PLUGIN_MANIFEST_ACTION", "\"$originalPluginManifestAction\"")
        buildConfigField("String", "ORIGINAL_DEBUG_PLUGIN_MANIFEST_ACTION", "\"$originalDebugPluginManifestAction\"")
        resValue("string", "ime_settings_activity", imeSettingsActivity)

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                targets(
                    // jni
                    "native-lib",
                    // copy fcitx5 built-in addon libraries
                    "copy-fcitx5-modules",
                    // android specific modules
                    "androidfrontend",
                    "androidkeyboard",
                    "androidnotification",
                    // built-in rime engine (fcitx5-rime), packaged as an app addon
                    "rime"
                )
            }
        }
    }

    flavorDimensions += "brand"
    productFlavors {
        create(flavorFx) {
            dimension = "brand"
            if (appIdFxSuffix.isNotEmpty()) applicationIdSuffix = appIdFxSuffix
            buildConfigField("boolean", "IS_FX_BUILD", "true")
        }
        create(flavorMainline) {
            dimension = "brand"
            applicationIdSuffix = appIdMainlineSuffix
            buildConfigField("boolean", "IS_FX_BUILD", "false")
        }
    }

    buildFeatures {
        viewBinding = true
        resValues = true
        compose = true
    }

    buildTypes {
        release {
            // app_icon / app_icon_round / app_name live in src/release/res instead of resValue():
            // resValue() declarations are re-applied to the androidTest variant, which then cannot
            // resolve the referenced resources and fails resource linking.
            proguardFile("proguard-rules.pro")
        }
        debug {
            // See the release block: declared in src/debug/res.
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

    fun fallbackAliasFromFxTask(taskName: String): String? = when {
        "FxDebug" in taskName -> taskName.replace("FxDebug", "Debug")
        "FxRelease" in taskName -> taskName.replace("FxRelease", "Release")
        else -> null
}

afterEvaluate {
    val fxTasks = tasks.names
        .filter { it.contains("FxDebug") || it.contains("FxRelease") }
        .toList()
    fxTasks.forEach { fxTaskName ->
        val alias = fallbackAliasFromFxTask(fxTaskName) ?: return@forEach
        if (tasks.findByName(alias) != null) return@forEach
        val fxTask = tasks.findByName(fxTaskName) ?: return@forEach
        tasks.register(alias) {
            group = fxTask.group
            description = "Alias of $fxTaskName"
            dependsOn(fxTaskName)
        }
    }
}

fun String.capitalized(): String = replaceFirstChar { c ->
    if (c.isLowerCase()) c.titlecase() else c.toString()
}

fun registerFxApkCompatCopy(buildType: String) {
    val buildTypeCap = buildType.capitalized()
    val assembleTask = "assembleFx$buildTypeCap"
    val compatTask = "syncFx${buildTypeCap}ApkToLegacyDir"
    tasks.register(compatTask) {
        dependsOn(assembleTask)
        doLast {
            // clear old APKs in target folder to avoid confusion (delete only .apk files)
            val destDir = layout.buildDirectory.dir("outputs/apk/$buildType").get().asFile
            if (destDir.exists()) {
                destDir.listFiles()?.filter { it.isFile && it.extension == "apk" }?.forEach { file ->
                    try {
                        file.delete()
                    } catch (_: Exception) {
                        // ignore deletion failures
                    }
                }
            } else {
                destDir.mkdirs()
            }

            // copy new fx APKs into legacy location
            copy {
                from(layout.buildDirectory.dir("outputs/apk/fx/$buildType"))
                into(destDir)
                include("*.apk")
            }
        }
    }
    tasks.matching { it.name == assembleTask }.configureEach {
        finalizedBy(compatTask)
    }
}

registerFxApkCompatCopy("debug")
registerFxApkCompatCopy("release")

androidComponents {
    beforeVariants(selector().withFlavor("brand" to flavorMainline)) { variantBuilder ->
        variantBuilder.enable = includeMainlineFlavor.get()
    }
    onVariants { variant ->
        when (variant.flavorName) {
            flavorMainline -> {
                val mainlineAppName = if (variant.buildType == "debug") {
                    appLabelMainlineDebug
                } else {
                    appLabelMainlineRelease
                }
                variant.manifestPlaceholders.put("appLabel", mainlineAppName)
                variant.outputs.forEach { output ->
                    if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                        output.outputFileName.set(
                            output.outputFileName.get().replace("-mainline-", "-")
                        )
                    }
                }
            }
            flavorFx -> {
                variant.outputs.forEach { output ->
                    if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                        output.outputFileName.set(
                            output.outputFileName.get()
                                .replace("$appIdBase-", "$appIdBase-fx-")
                                .replace("-fx-", "-")
                        )
                    }
                }
            }
        }
    }
}

fcitxComponent {
    includeLibs = listOf(
        "fcitx5",
        "fcitx5-lua",
        "libime",
        "fcitx5-chinese-addons"
    )
    // exclude (delete immediately after install) tables that nobody would use
    excludeFiles = listOf("cangjie", "erbi", "qxm", "wanfeng").map {
        "usr/share/fcitx5/inputmethod/$it.conf"
    } + listOf(
        // pinyin language model is shipped by the :plugin:pinyin-lm plugin instead
        "usr/share/libime/zh_CN.lm",
        "usr/share/libime/zh_CN.lm.predict",
        // table input methods and dictionaries are shipped by the :plugin:table-data plugin instead
        "usr/share/fcitx5/inputmethod/db.conf",
        "usr/share/fcitx5/inputmethod/wbpy.conf",
        "usr/share/fcitx5/inputmethod/wbx.conf",
        "usr/share/fcitx5/inputmethod/zrm.conf",
        "usr/share/fcitx5/table/db.main.dict",
        "usr/share/fcitx5/table/wbpy.main.dict",
        "usr/share/fcitx5/table/wbx.main.dict",
        "usr/share/fcitx5/table/zrm.main.dict"
    )
    installPrebuiltAssets = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    ksp(project(":codegen"))
    implementation(project(":lib:fcitx5"))
    implementation(project(":lib:fcitx5-lua"))
    implementation(project(":lib:libime"))
    implementation(project(":lib:fcitx5-chinese-addons"))
    implementation(project(":lib:common"))
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.preference)
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.androidx.recyclerview)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.arrow.core)
    implementation(libs.arrow.functions)
    implementation(libs.imagecropper)
    implementation(libs.flexbox)
    implementation(libs.dependency)
    implementation(libs.timber)
    implementation(libs.splitties.bitflags)
    implementation(libs.splitties.dimensions)
    implementation(libs.splitties.resources)
    implementation(libs.splitties.views.dsl)
    implementation(libs.splitties.views.dsl.appcompat)
    implementation(libs.splitties.views.dsl.constraintlayout)
    implementation(libs.splitties.views.dsl.coordinatorlayout)
    implementation(libs.splitties.views.dsl.recyclerview)
    implementation(libs.splitties.views.recyclerview)
    implementation(libs.aboutlibraries.core)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.xz)
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation(libs.pictureselector)
    implementation(libs.androidsvg)
    // Compose + miuix: the settings UI is written with the same component library that
    // KernelSU-Style-UI-Kit uses, so the look matches without a second design system.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
    // Material 3 (+ materialKolor for the palette generation) backs the "Material" UI style the
    // template offers next to miuix; see UiMode and FcitxTheme.
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.kolor)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.lifecycle.testing)
    androidTestImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

configurations {
    all {
        // remove Baseline Profile Installer or whatever it is...
        exclude(group = "androidx.profileinstaller", module = "profileinstaller")
        // remove unwanted splitties libraries...
        exclude(group = "com.louiscad.splitties", module = "splitties-appctx")
        exclude(group = "com.louiscad.splitties", module = "splitties-systemservices")
    }
}
