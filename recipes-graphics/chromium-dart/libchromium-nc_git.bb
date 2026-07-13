SUMMARY = "chromium_dart native backend (CEF + wayland-cxx-scanner) -- libchromium_nc.so"
DESCRIPTION = "The FFI/CEF backend for the chromium_dart Flutter package: renders \
Chromium (CEF windowless OSR) into a Wayland subsurface for in-scene embedding by \
ivi-homescreen. dlopen()ed by the chromium_dart_view bridge plugin (CHROMIUM_DART_LIB)."
SECTION = "graphics"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRCREV = "cbe521c72df7cbea6eb10b969d52e19665866d89"
SRC_URI = "gitsm://github.com/AhmedAdelWafdy7/chromium_dart.git;protocol=https;branch=main"
S = "${UNPACKDIR}/${BP}/native"

# CEF comes from the cef recipe (recipes-graphics/chromium-dart/cef_128.bb) --
# no depot_tools auto-fetch actually exists in this build chain (verified
# directly against ivi-homescreen + the webview_flutter_view plugin sources;
# see flutter-auto_2.0.bbappend). Points CEF_ROOT / CEF_WRAPPER_LIB at the
# exact same ${datadir}/cef dist the webview plugin uses.
DEPENDS = "cef wayland wayland-protocols wayland-cxx-scanner-native"
REQUIRED_DISTRO_FEATURES = "wayland"

# LLVM/libc++ toolchain, matching flutter-auto (see flutter-auto_2.0.bbappend --
# mixing libstdc++/libc++ across the CEF wrapper boundary is an ABI break).
TOOLCHAIN = "clang"
TC_CXX_RUNTIME = "llvm"
LIBCPLUSPLUS = "-stdlib=libc++"

inherit cmake features_check pkgconfig

EXTRA_OECMAKE = "\
    -DCHROMIUM_DART_WITH_CEF=ON \
    -DCEF_ROOT=${STAGING_DIR_TARGET}${datadir}/cef \
    -DCEF_WRAPPER_LIB=${STAGING_LIBDIR}/libcef_dll_wrapper.a \
    -DWAYLAND_CXX_DIR=${STAGING_DATADIR_NATIVE}/wayland-cxx-scanner \
    -DWAYLAND_CXX_SCANNER_EXECUTABLE=${STAGING_BINDIR_NATIVE}/wayland-cxx-scanner \
    -DCMAKE_POSITION_INDEPENDENT_CODE=ON \
"

do_install() {
    # CMakeLists.txt sets LIBRARY_OUTPUT_DIRECTORY to "${CMAKE_SOURCE_DIR}/../lib"
    # -- CMAKE_SOURCE_DIR is ${S} (native/), not the build dir ${B} -- so the
    # .so lands at ${S}/../lib, a sibling of native/, not under ${B}.
    install -d ${D}${libdir}
    install -m 0755 ${S}/../lib/libchromium_nc.so ${D}${libdir}/libchromium_nc.so
    # CEF single-binary subprocess helper (browser_subprocess_path).
    install -d ${D}${libexecdir}/chromium_dart
    install -m 0755 ${B}/chromium_dart_subprocess \
        ${D}${libexecdir}/chromium_dart/ || true
}

FILES:${PN} = "${libdir}/libchromium_nc.so ${libexecdir}/chromium_dart"
# libchromium_nc.so is a dlopen()'d runtime plugin, not a link-time dev
# artifact -- there's no legitimate -dev package here (no headers/.a/.pc for
# consumers). Without this, OE's default packaging claims any *unversioned*
# ${libdir}/*.so for ${PN}-dev before my explicit FILES:${PN} above gets a
# chance at it (dev-elf QA: "-dev package contains non-symlink .so").
FILES:${PN}-dev = ""
INSANE_SKIP:${PN} += "dev-so"

# CHROMIUM_DART_WITH_CEF's CEF_ROOT is used for two different things in
# chromium_dart's own CMakeLists.txt: the -I compile-time include dir (needs
# the sysroot-prefixed build path, correct here) AND
# target_compile_definitions(... CHROMIUM_DART_CEF_ROOT="${CEF_ROOT}") -- a
# runtime string baked verbatim into the binary, which ends up as a literal
# TMPDIR/sysroot path that won't exist on the real device (buildpaths QA).
# Confirmed directly in bridge_cef.cpp/cef_subprocess.cpp: both read a
# CEF_ROOT *environment variable* first and only fall back to the baked
# string if unset -- so the wrong baked default is harmless as long as
# CEF_ROOT=/usr/share/cef is set at launch (see
# recipes-graphics/flutter-auto/files/flutter-auto-ivi-app.service).
INSANE_SKIP:${PN} += "buildpaths"
INSANE_SKIP:${PN}-dbg += "buildpaths"

# -DWAYLAND_CXX_DIR= expects the wayland-cxx-scanner-native recipe's staged
# cmake/+include/ tree under ${STAGING_DATADIR_NATIVE}/wayland-cxx-scanner --
# confirmed correct directly against chromium_dart/native/CMakeLists.txt (see
# recipes-devtools/wayland-cxx-scanner's own do_install:append()). ANGLE/
# SwiftShader are already stripped from the CEF Release dir by cef_128.bb's
# own do_install (GPU-less target, software OSR only).
