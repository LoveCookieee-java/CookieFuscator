cmake_minimum_required(VERSION 3.10)
project(AntiOpsecNativeShield CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS} include)

set(SOURCES src/NativeBridge.cpp)

add_library(antiopsec_x64 SHARED ${SOURCES})

target_include_directories(antiopsec_x64 PRIVATE ${JNI_INCLUDE_DIRS} include)

# Apply Maximum Code Stripping & Optimization
if(MSVC)
    target_compile_options(antiopsec_x64 PRIVATE /O2 /Oi /GL /Gy)
    target_link_options(antiopsec_x64 PRIVATE /LTCG /OPT:REF /OPT:ICF)
else()
    target_compile_options(antiopsec_x64 PRIVATE -O3 -s -fvisibility=hidden -fomit-frame-pointer)
    target_link_options(antiopsec_x64 PRIVATE -s -Wl,--gc-sections)
endif()
