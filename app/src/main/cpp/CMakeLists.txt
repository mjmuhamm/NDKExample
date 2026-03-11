cmake_minimum_required(VERSION 3.22.1)
project("native_lib")

add_library(native_lib SHARED native-lib.cpp)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics) # required for bitmap manipulation

target_link_libraries(native_lib ${log-lib} ${jnigraphics-lib})