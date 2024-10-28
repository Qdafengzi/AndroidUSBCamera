# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)


add_library( # Sets the name of the library.
        yuv-decoder

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        yuv-decoder.c)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

## Set OpenMP flags
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp")
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fopenmp")
#
## Link against OpenMP libraries
#find_package(OpenMP REQUIRED)
#if (OPENMP_FOUND)
#    target_link_libraries(yuv-decoder OpenMP::OpenMP_CXX)
#endif()

target_link_libraries( # Specifies the target library.
        yuv-decoder

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        GLESv2
        jnigraphics)
