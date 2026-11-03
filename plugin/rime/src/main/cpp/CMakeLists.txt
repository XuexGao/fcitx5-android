cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-rime VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

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

# https://github.com/rime/librime/blob/1.9.0/src/rime_api.h#L663
# https://stackoverflow.com/questions/805555/ld-linker-question-the-whole-archive-option
# https://cmake.org/cmake/help/latest/manual/cmake-generator-expressions.7.html#link-features
set(RIME_TARGET $<LINK_LIBRARY:WHOLE_ARCHIVE,Rime_static>)
set(RIME_DATA_DIR "${CMAKE_INSTALL_DATADIR}/rime-data")
add_subdirectory(fcitx5-rime)

# ---------------------------------------------------------------------------
# rime-wanxiang (万象拼音) — default Chinese schema source.
#
# The submodule is pinned in .gitmodules. Its schemas, Lua processors and the
# required dictionaries are installed into the same `rime-data` tree as the
# stock luna_pinyin data, so the app's normal data-descriptor deployment copies
# them to the device on first run without any extra step.
#
# Configure with -DINCLUDE_WANXIANG_DICT=OFF to ship the schemas without the
# ~54 MB dictionaries (useful for size-constrained builds; the schema then
# falls back to the bundled luna_pinyin data).
# ---------------------------------------------------------------------------
set(WANXIANG_DIR "${CMAKE_CURRENT_SOURCE_DIR}/rime-wanxiang")
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

install(FILES
        "default.yaml"
        "rime-essay/essay.txt"
        "rime-prelude/key_bindings.yaml"
        "rime-prelude/punctuation.yaml"
        "rime-prelude/symbols.yaml"
        "rime-luna-pinyin/luna_pinyin_fluency.schema.yaml"
        "rime-luna-pinyin/luna_pinyin_simp.schema.yaml"
        "rime-luna-pinyin/luna_pinyin_tw.schema.yaml"
        "rime-luna-pinyin/luna_pinyin.dict.yaml"
        "rime-luna-pinyin/luna_pinyin.schema.yaml"
        "rime-luna-pinyin/luna_quanpin.schema.yaml"
        "rime-luna-pinyin/pinyin.yaml"
        "rime-stroke/stroke.dict.yaml"
        "rime-stroke/stroke.schema.yaml"
        DESTINATION "${FCITX_INSTALL_DATADIR}/rime-data"
        COMPONENT prebuilt-assets
        )
