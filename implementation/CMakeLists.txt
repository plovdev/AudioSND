cmake_minimum_required(VERSION 3.20)
project(AudioSND VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(RESOURCES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/main/resources/natives/libs")
file(MAKE_DIRECTORY "${RESOURCES_DIR}")

# ==== ПОИСК JNI ============================================
# Пытаемся найти JNI автоматически
find_package(JNI QUIET)

# Если не нашли автоматически, используем переданные пути
if(NOT JNI_FOUND)
    if(JNI_INCLUDE_PATH)
        message(STATUS "Используем JNI_INCLUDE_PATH: ${JNI_INCLUDE_PATH}")
        set(JNI_INCLUDE_DIRS ${JNI_INCLUDE_PATH})

        # Для macOS добавляем также darwin поддиректорию
        if(APPLE AND JNI_INCLUDE_DARWIN)
            message(STATUS "Используем JNI_INCLUDE_DARWIN: ${JNI_INCLUDE_DARWIN}")
            list(APPEND JNI_INCLUDE_DIRS ${JNI_INCLUDE_DARWIN})
        endif()

        # Для Linux добавляем linux поддиректорию
        if(LINUX AND JNI_INCLUDE_LINUX)
            message(STATUS "Используем JNI_INCLUDE_LINUX: ${JNI_INCLUDE_LINUX}")
            list(APPEND JNI_INCLUDE_DIRS ${JNI_INCLUDE_LINUX})
        endif()

        set(JNI_FOUND TRUE)
    else()
        message(WARNING "JNI не найден, пытаемся найти в системе...")
        # Пробуем найти через find_path
        find_path(JNI_INCLUDE_DIR jni.h
            PATHS
                /usr/lib/jvm/java-*/include
                /usr/local/java/include
                /opt/java/include
                /Library/Java/JavaVirtualMachines/*/Contents/Home/include
                "C:/Program Files/Java/*/include"
            DOC "Путь к заголовкам JNI"
        )

        if(JNI_INCLUDE_DIR)
            set(JNI_INCLUDE_DIRS ${JNI_INCLUDE_DIR})

            if(APPLE)
                find_path(JNI_INCLUDE_DARWIN_DIR jni_md.h
                    PATHS ${JNI_INCLUDE_DIR}/darwin
                )
                if(JNI_INCLUDE_DARWIN_DIR)
                    list(APPEND JNI_INCLUDE_DIRS ${JNI_INCLUDE_DARWIN_DIR})
                endif()
            endif()

            if(LINUX)
                find_path(JNI_INCLUDE_LINUX_DIR jni_md.h
                    PATHS ${JNI_INCLUDE_DIR}/linux
                )
                if(JNI_INCLUDE_LINUX_DIR)
                    list(APPEND JNI_INCLUDE_DIRS ${JNI_INCLUDE_LINUX_DIR})
                endif()
            endif()

            set(JNI_FOUND TRUE)
        else()
            message(FATAL_ERROR "JNI не найден! Установите JDK или укажите JNI_INCLUDE_PATH")
        endif()
    endif()
endif()

message(STATUS "JNI include directories: ${JNI_INCLUDE_DIRS}")

add_library(AudioSND SHARED)

if(APPLE)
    target_link_libraries(AudioSND "-framework AudioToolbox -framework CoreAudio -framework CoreFoundation")
endif()

add_library(AudioSND::AudioSND ALIAS AudioSND)

file(GLOB_RECURSE LIB_SOURCES "*.cpp")
target_sources(AudioSND PRIVATE ${LIB_SOURCES})
target_include_directories(AudioSND PRIVATE ${JNI_INCLUDE_DIRS})

target_include_directories(AudioSND PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)

set_target_properties(AudioSND PROPERTIES
            VERSION ""
            SOVERSION ""
            OUTPUT_NAME "audio-snd"
        )

if(WIN32)
    target_compile_definitions(AudioSND PRIVATE AudioSND_EXPORTS)
    set_target_properties(AudioSND PROPERTIES
        WINDOWS_EXPORT_ALL_SYMBOLS TRUE
    )
elseif(APPLE)
    set_target_properties(AudioSND PROPERTIES
        INSTALL_NAME_DIR "@rpath"
        BUILD_WITH_INSTALL_RPATH TRUE
        MACOSX_RPATH TRUE
    )
elseif(UNIX AND NOT APPLE)
    target_compile_options(AudioSND PRIVATE -fvisibility=hidden -fPIC)
endif()

add_custom_command(TARGET AudioSND POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
        "$<TARGET_FILE:AudioSND>"
        "${RESOURCES_DIR}/libaudio-snd.dylib"
    COMMENT "Копирование библиотеки в resources/natives/libs"
)