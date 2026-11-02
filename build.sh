#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_dependencies() {
    local deps=("cmake" "make" "g++")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            print_error "$dep not found."
            exit 1
        fi
    done
    print_info "All dependencies installed"
}

create_dirs() {
    mkdir -p ./build
    mkdir -p implementation/src/main/resources/natives/libs
}

find_cpp_files() {
    print_info "Поиск C++ файлов для CMake..."
    find . -name "*.cpp" > ./build/natives.txt
    local count=$(wc -l < ./build/natives.txt)
    print_info "  Найдено $count C++ файлов"
}

compile_native_cmake() {
    print_info "Компиляция нативной библиотеки..."

    local os_name=$(uname -s)
    local jni_include_path=""
    local jni_include_darwin=""
    local jni_include_linux=""

    if [[ "$os_name" == "Darwin" ]]; then
        jni_include_path="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/include"
        jni_include_darwin="$jni_include_path/darwin"
        print_info "  Платформа: macOS"
        print_info "  JNI include path: $jni_include_path"
        print_info "  JNI darwin include: $jni_include_darwin"
    elif [[ "$os_name" == "Linux" ]]; then
        jni_include_path="/usr/lib/jvm/java-21-openjdk/include"
        jni_include_linux="$jni_include_path/linux"
        print_info "  Платформа: Linux"
    else
        print_error "  Неподдерживаемая платформа: $os_name"
        exit 1
    fi

    if [ ! -d "$jni_include_path" ]; then
        print_warn "  JNI include path не найден: $jni_include_path"
        print_warn "  Попытка найти JDK автоматически..."

        if command -v java &> /dev/null; then
            local java_home=$(java -XshowSettings:properties -version 2>&1 | grep java.home | awk -F'=' '{print $2}' | tr -d ' ')
            jni_include_path="$java_home/include"

            if [[ "$os_name" == "Darwin" ]]; then
                jni_include_darwin="$jni_include_path/darwin"
            elif [[ "$os_name" == "Linux" ]]; then
                jni_include_linux="$jni_include_path/linux"
            fi

            print_info "  Найден JDK по пути: $java_home"
        fi
    fi

    if [[ "$os_name" == "Darwin" ]] && [ ! -d "$jni_include_darwin" ]; then
        print_error "  Не найден darwin JNI include: $jni_include_darwin"
        print_error "  Проверьте установку JDK"
        return 1
    fi

    cd ./build || exit
    print_info "  Конфигурация CMake..."
    if [[ "$os_name" == "Darwin" ]]; then
        cmake ../implementation \
            -DJNI_INCLUDE_PATH="$jni_include_path" \
            -DJNI_INCLUDE_DARWIN="$jni_include_darwin" \
            2>&1 | tee ../build/cmake_configure.log
    else
        cmake ../implementation \
            -DJNI_INCLUDE_PATH="$jni_include_path" \
            -DJNI_INCLUDE_LINUX="$jni_include_linux" \
            2>&1 | tee ../build/cmake_configure.log
    fi

    if [ $? -eq 0 ]; then
        print_info "CMake сконфигурирован"
    else
        print_error "Ошибка конфигурации CMake"
        cd ..
        return 1
    fi

    print_info "  Сборка нативной библиотеки..."
    cmake --build . 2>&1 | tee ../build/cmake_build.log

    local build_result=$?
    if [ $build_result -eq 0 ]; then
        print_info "    ✓ Нативная библиотека собрана"

        if [[ "$os_name" == "Darwin" ]]; then
            if [ -f "../implementation/src/main/resources/natives/libs/audio-snd.dylib" ]; then
                print_info "Библиотека создана: implementation/src/main/resources/natives/libs/audio-snd.dylib"
            else
                print_warn "Библиотека не найдена в ожидаемом месте"
            fi
        elif [[ "$os_name" == "Linux" ]]; then
            if [ -f "../implementation/src/main/resources/natives/libs/audio-snd.so" ]; then
                print_info "Библиотека создана: implementation/src/main/resources/natives/libs/audio-snd.so"
            fi
        fi
    else
        print_error "Ошибка сборки нативной библиотеки"
        print_info "Проверьте логи в builds/cmake_build.log"
    fi
    
    # Возвращаемся обратно
    cd ..
}

# Основная функция
main() {
    print_info "Начало сборки проекта..."
    
    check_dependencies
    create_dirs
    find_java_files
    find_cpp_files
    
    # Компиляция Java
    compile_java_modules
    
    # Компиляция нативной библиотеки с CMake
    compile_native_cmake

    if [ $? -ne 0 ]; then
        print_warn "CMake сборка не удалась..."
    fi
    
    print_info "Сборка завершена!"

    # Вывод итоговой информации
    echo ""
    echo "================== РЕЗУЛЬТАТЫ =================="

    echo ""
    echo "Нативные библиотеки:"
    if [[ "$(uname -s)" == "Darwin" ]]; then
        ls -la implementation/src/main/resources/natives/libs/*.dylib 2>/dev/null || echo "  Не найдены"
    elif [[ "$(uname -s)" == "Linux" ]]; then
        ls -la implementation/src/main/resources/natives/libs/*.so 2>/dev/null || echo "  Не найдены"
    fi
    echo "================================================"

    rm audio-snd.dylib;
    rm audio-snd.so;
    rm audio-snd.dll;
}

main "$@"