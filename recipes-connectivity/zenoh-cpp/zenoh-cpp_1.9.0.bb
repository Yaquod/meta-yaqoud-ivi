SUMMARY = "C++ bindings for Zenoh (zenohcxx)"
DESCRIPTION = "Header-only C++ API for the Eclipse Zenoh protocol, layered on \
top of zenoh-c. Required by iv-cloud-gateway (find_package(zenohcxx))."
HOMEPAGE = "https://github.com/eclipse-zenoh/zenoh-cpp"
LICENSE = "Apache-2.0 & EPL-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=530d837aca648e45704db71dedff39c4"

# ZENOHCXX_ZENOHC=ON triggers find_package(zenohc REQUIRED) in zenoh-cpp's own
# CMakeLists.txt -- matches the "zenohc" CMake package our zenoh-c recipe installs.
DEPENDS = "zenoh-c"

inherit cmake

SRC_URI = "git://github.com/eclipse-zenoh/zenoh-cpp.git;protocol=https;branch=main"
SRCREV = "78a5ab9c0130ed65f44d7bd1958398039fb14163"

EXTRA_OECMAKE = " \
    -DZENOHCXX_ZENOHC=ON \
    -DZENOHCXX_ZENOHPICO=OFF \
    -DZENOHCXX_ENABLE_TESTS=OFF \
    -DZENOHCXX_ENABLE_EXAMPLES=OFF \
    -DZENOHCXX_EXAMPLES_PROTOBUF=OFF \
"

# Header-only: nothing lands in the default (runtime) FILES:${PN} match, only
# in -dev (headers, zenohcxxConfig*.cmake, pkgconfig). Never image-installed
# directly, only pulled in at build time via DEPENDS.
ALLOW_EMPTY:${PN} = "1"
