SUMMARY = "A Flutter Cluster application"
DESCRIPTION = "An Example Flutter application for the cluster"
AUTHOR = "Ahmed Wafdy"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRCREV = "74744e55798c1cf08b58582f3cccdc122b726fc4"
SRC_URI = "git://github.com/Yaquod/flutter_cluster.git;lfs=0;branch=zenoh-package;protocol=https \
           file://.env \
            file://config.toml \
"

PUBSPEC_APPNAME = "flutter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "yaqoud-flutter-cluster"
PUBSPEC_IGNORE_LOCKFILE = "1"

DEPENDS:append = " zenoh-c"

inherit cmake flutter-app pkgconfig

do_configure:append() {
    cp ${UNPACKDIR}/.env ${S}/.env
}

OECMAKE_SOURCEPATH = "${S}/native/zenoh_bridge"

EXTRA_OECMAKE += "\
    -DDART_SDK=${STAGING_DIR_NATIVE}/usr/share/flutter/sdk/bin/cache/dart-sdk \
"

python do_cmake_compile() {
    bb.build.exec_func('cmake_do_compile', d)
}
addtask cmake_compile after do_compile before do_install
do_cmake_compile[dirs] = "${B}"

do_install:append() {
    install -d ${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/${FLUTTER_RUNTIME_MODE}/lib
    install -m 755 ${B}/libcluster_bridge.so \
        ${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/${FLUTTER_RUNTIME_MODE}/lib/
}

FILES:${PN} += "\
    ${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/${FLUTTER_RUNTIME_MODE}/lib/libcluster_bridge.so \
"

# The bridge library is intentionally bundled with the Flutter app rather than 
# installed system-wide in /usr/lib. Skip the libdir QA check to allow this.
INSANE_SKIP:${PN} += "libdir"
INSANE_SKIP:${PN}-dbg += "libdir"
