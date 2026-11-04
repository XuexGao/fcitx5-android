cmake_minimum_required(VERSION 3.18)

# ---------------------------------------------------------------------------
# fcitx5-rime built straight into the app.
#
# This mirrors plugin/rime's CMakeLists (same librime prebuilt, same rime data and the
# rime-wanxiang schema set), but defines the addon target inside the :app native build. As a
# result the Rime engine ships with the main APK: no separate plugin has to be installed.
#
# The addon .so is packaged as an app native library, which is where fcitx5 looks for built-in
# addons (FCITX_ADDON_DIRS points at the app lib dir), and the rime data/configuration is emitted
# through the same `prebuilt-assets` / `config` install components the app already deploys.
# ---------------------------------------------------------------------------

set(RIME_PLUGIN_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../plugin/rime/src/main/cpp")

if (NOT EXISTS "${RIME_PLUGIN_DIR}/fcitx5-rime/src/CMakeLists.txt")
    message(WARNING "fcitx5-rime sources are missing; building without the built-in Rime engine")
    return()
endif ()

set(Rime_VERSION "1.12.0")
set(Rime_dependencies
        log # glog links to android log
        "${PREBUILT_DIR}/glog/${ANDROID_ABI}/lib/libglog.a"
        "${PREBUILT_DIR}/leveldb/${ANDROID_ABI}/lib/libleveldb.a"
        "${PREBUILT_DIR}/lua/${ANDROID_ABI}/lib/liblua_static.a"
        "${PREBUILT_DIR}/marisa/${ANDROID_ABI}/lib/libmarisa.a"
        "${PREBUILT_DIR}/opencc/${ANDROID_ABI}/lib/libopencc.a"
        "${PREBUILT_DIR}/yaml-cpp/${ANDROID_ABI}/lib/libyaml-cpp.a"
        )

add_library(Rime_static STATIC IMPORTED)
set_target_properties(Rime_static PROPERTIES
        IMPORTED_LOCATION "${PREBUILT_DIR}/librime/${ANDROID_ABI}/lib/librime.a"
        INTERFACE_INCLUDE_DIRECTORIES "${PREBUILT_DIR}/librime/${ANDROID_ABI}/include"
        INTERFACE_LINK_LIBRARIES "${Rime_dependencies}"
        )

set(RIME_TARGET $<LINK_LIBRARY:WHOLE_ARCHIVE,Rime_static>)

# The addon sources reference ${RIME_TARGET}/RIME_DATA_DIR from their parent scope.
set(RIME_DATA_DIR "${CMAKE_INSTALL_DATADIR}/rime-data")
add_subdirectory("${RIME_PLUGIN_DIR}/fcitx5-rime" fcitx5-rime)

# ---- stock rime data -------------------------------------------------------
install(FILES
        "${RIME_PLUGIN_DIR}/default.yaml"
        "${RIME_PLUGIN_DIR}/rime-essay/essay.txt"
        "${RIME_PLUGIN_DIR}/rime-prelude/key_bindings.yaml"
        "${RIME_PLUGIN_DIR}/rime-prelude/punctuation.yaml"
        "${RIME_PLUGIN_DIR}/rime-prelude/symbols.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_pinyin_fluency.schema.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_pinyin_simp.schema.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_pinyin_tw.schema.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_pinyin.dict.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_pinyin.schema.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/luna_quanpin.schema.yaml"
        "${RIME_PLUGIN_DIR}/rime-luna-pinyin/pinyin.yaml"
        "${RIME_PLUGIN_DIR}/rime-stroke/stroke.dict.yaml"
        "${RIME_PLUGIN_DIR}/rime-stroke/stroke.schema.yaml"
        DESTINATION "${FCITX_INSTALL_DATADIR}/rime-data"
        COMPONENT prebuilt-assets
        )

# ---- rime-wanxiang (万象拼音) ----------------------------------------------
set(WANXIANG_DIR "${RIME_PLUGIN_DIR}/rime-wanxiang")
option(INCLUDE_WANXIANG_DICT "Bundle the rime-wanxiang dictionaries" ON)
if (EXISTS "${WANXIANG_DIR}/wanxiang.schema.yaml")
    file(GLOB WANXIANG_YAML "${WANXIANG_DIR}/wanxiang*.yaml")
    install(FILES
            ${WANXIANG_YAML}
            "${WANXIANG_DIR}/custom_phrase.dict.yaml"
            "${WANXIANG_DIR}/version.txt"
            DESTINATION "${FCITX_INSTALL_DATADIR}/rime-data"
            COMPONENT prebuilt-assets
            )
    install(DIRECTORY
            "${WANXIANG_DIR}/lua"
            "${WANXIANG_DIR}/opencc"
            DESTINATION "${FCITX_INSTALL_DATADIR}/rime-data"
            COMPONENT prebuilt-assets
            )
    if (INCLUDE_WANXIANG_DICT)
        install(DIRECTORY
                "${WANXIANG_DIR}/dicts"
                DESTINATION "${FCITX_INSTALL_DATADIR}/rime-data"
                COMPONENT prebuilt-assets
                )
    endif ()
else ()
    message(WARNING "rime-wanxiang submodule is missing; building without it")
endif ()
