cmake_minimum_required(VERSION 3.4.1)

# Make executables
add_executable(arp-spoof src/main/cpp/arp-spoof.cpp )
add_executable(dns-spoof src/main/cpp/dns-spoof.cpp )
add_executable(scanner src/main/cpp/scanner.cpp )

# Not a shared object but android studio only packages native components
# into apk if their name is lib.*so
set_target_properties( dns-spoof PROPERTIES OUTPUT_NAME "libdnsspoof.so" )
set_target_properties( arp-spoof PROPERTIES OUTPUT_NAME "libarpspoof.so" )
set_target_properties( scanner PROPERTIES OUTPUT_NAME "libscanner.so" )

###################### LIBTINS ######################
add_library(tins SHARED IMPORTED)

set_target_properties( tins PROPERTIES IMPORTED_LOCATION
                       # Provides the path to the library you want to import.
                       /home/nibiru/repos/Evil-AP/app/src/main/jniLibs/armeabi-v7a/libtins.so)

include_directories( /home/nibiru/repos/Evil-AP/app/src/main/cpp/include/ )

###################### NATIVE-LIB ######################
# Include libraries needed for native component
target_link_libraries(arp-spoof android log tins )
target_link_libraries(dns-spoof android log tins )
target_link_libraries(scanner android log tins )


