SUMMARY = "Chromium Embedded Framework 128 (prebuilt minimal dist) + wrapper"
DESCRIPTION = "CEF binary distribution used by the webview_flutter_view plugin and \
the chromium_dart backend. Ships libcef.so + resources; builds libcef_dll_wrapper.a."
SECTION = "graphics"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=88f49d5225b9d3deadcaacb8a0b4d7d7"

# The dylib/header/CMakeLists patches below are fetched verbatim from
# meta-flutter/workspace-automation's own patch set (an external, already
# published third-party repo), not authored in this layer -- they don't carry
# the OE convention's own "Upstream-Status:" tag, which do_patch's QA check
# otherwise treats as fatal. Downgrading rather than fabricating that tag into
# someone else's patch file.
ERROR_QA:remove = "patch-status"

# There is no depot_tools/gclient auto-fetch anywhere in this build chain --
# checked directly: neither ivi-homescreen's own CMakeLists.txt/cmake/options.cmake
# (HOMESCREEN_COMMIT dd6d9224 pinned by flutter-auto_2.0.bb) nor the
# webview_flutter_view plugin's CMakeLists.txt (PLUGINS_COMMIT e16dee0a, our fork)
# reference "depot" or invoke gclient anywhere. The plugin's CMakeLists.txt just
# hard-requires an already-unpacked CEF_ROOT
# (`if (NOT EXISTS ${CEF_ROOT}) message(FATAL_ERROR ...)`, then
# `add_subdirectory(${CEF_ROOT})` + links `${CEF_ROOT}/${CEF_BUILD_TYPE}`). This
# prebuilt-tarball recipe is therefore the actual (not just fallback) CEF source
# for both flutter-auto_2.0.bbappend and libchromium-nc_git.bb -- both DEPENDS on
# this and point -DCEF_ROOT= at the same ${datadir}/cef it installs to.
#
# Pinned to the exact build the plugin was validated against.
CEF_VER = "128.4.9+g9840ad9+chromium-128.0.6613.120"

# chromium_dart's bridge_cef.cpp/cef_subprocess.cpp #include
# "include/wrapper/cef_library_loader.h" and call cef_load_library() -- that
# header/implementation genuinely doesn't exist in upstream CEF for Linux at
# all (upstream only ships it for OS_MAC; confirmed directly in CEF's own
# source). meta-flutter/workspace-automation (the reference build for this
# exact ivi-homescreen/webview_flutter_view/chromium_dart stack, see
# https://github.com/meta-flutter/workspace-automation) hand-vendors a
# portable dlopen()-based implementation onto the prebuilt minimal dist via
# these patches -- fetched from there directly rather than retyped here,
# since 0003 in particular is a large CEF-generated file.
SRC_URI = "\
    https://cef-builds.spotifycdn.com/cef_binary_${CEF_VER}_linuxarm64_minimal.tar.bz2 \
    https://raw.githubusercontent.com/AhmedAdelWafdy7/workspace-automation/feature/cef-agl-multiarch/patches/cef-prebuilt/0003-add-dylib.patch;name=dylib \
    https://raw.githubusercontent.com/AhmedAdelWafdy7/workspace-automation/feature/cef-agl-multiarch/patches/cef-prebuilt/0004-add-dylib-header.patch;name=dylibheader \
    https://raw.githubusercontent.com/AhmedAdelWafdy7/workspace-automation/feature/cef-agl-multiarch/patches/cef-prebuilt/0005-include-dylib.patch;name=includedylib \
    https://raw.githubusercontent.com/AhmedAdelWafdy7/workspace-automation/feature/cef-agl-multiarch/patches/cef-prebuilt/0007-fix-werror-unused-arg.patch;name=werrorfix \
"
SRC_URI[sha256sum] = "1c54328515c3e70c90c85349f76b4b00d16d6464bd08ccb455fe7e92dd86567c"
SRC_URI[dylib.sha256sum] = "f55b9c386613b9efed2731606540f10fbc182d0b1544b165bbc19f3c0a8c94c3"
SRC_URI[dylibheader.sha256sum] = "9e1fa642f1c59ac99f4449e5b0ffdf5facc32d897f63cbab27ccb4081d8bbb97"
SRC_URI[includedylib.sha256sum] = "25fba9d4f054bbdf21986f9a4e589df73f7ce3dca4a31755421f18974340adeb"
SRC_URI[werrorfix.sha256sum] = "72866dd54463f422076859f179d85daa2d680f0118545b2256cde771c882a896"

S = "${UNPACKDIR}/cef_binary_${CEF_VER}_linuxarm64_minimal"

DEPENDS = "wayland"
TOOLCHAIN = "clang"
TC_CXX_RUNTIME = "llvm"
LIBCPLUSPLUS = "-stdlib=libc++"

inherit cmake

# Build only the DLL wrapper; the rest of the dist is prebuilt binaries we stage.
#
# -DPROJECT_ARCH=arm64: without this, cmake/cef_variables.cmake picks the arch
# from CMAKE_HOST_SYSTEM_PROCESSOR (the build host, x86_64) rather than the
# cross-compile target, and hardcodes -march=x86-64 -- which the aarch64
# cross clang rejects outright ("unsupported argument 'x86-64' to option
# '-march='"). Confirmed directly in that file: the arch-flags elseif chain
# only branches on "x86_64" / "x86", nothing for "arm64", so setting this
# just makes CEF add no conflicting flags at all and defer entirely to the
# cross-toolchain's own (already-correct) -march=armv8-a+crc.
EXTRA_OECMAKE = "\
    -DPROJECT_ARCH=arm64 \
    -DCEF_RUNTIME_LIBRARY_FLAG=-stdlib=libc++ \
"

do_compile() {
    cmake -S ${S} -B ${B}
    cmake --build ${B} --target libcef_dll_wrapper
}

do_install() {
    # ${datadir}/cef ships the ENTIRE raw tarball extraction (CMakeLists.txt,
    # cmake/, libcef_dll/ wrapper source, libcef_dll_wrapper/, include/,
    # Release/, Resources/, LICENSE.txt...), not just a hand-picked subset --
    # confirmed against the reference this whole CEF integration is modeled on
    # (AhmedAdelWafdy7/workspace-automation, configs/cef-prebuilt.json: its
    # CEF_ROOT is just `tar xjf` + the same patches applied in place, nothing
    # subsetted). The webview_flutter_view plugin's CMakeLists.txt does
    # `add_subdirectory(${CEF_ROOT} ...)`, which re-runs CEF's own
    # CMakeLists.txt in-tree to (re)define the libcef_dll_wrapper CMake
    # target it links against -- that only works if CEF_ROOT/CMakeLists.txt
    # and CEF_ROOT/libcef_dll/*.cc actually exist, which the old
    # Release+Resources+include-only install didn't provide.
    #
    # NOTE: cp -r, not -a -- -a preserves the source files' real uid/gid (the
    # tarball was unpacked as the build user), which fakeroot/pseudo then
    # bakes into the packaged output as a literal uid instead of root,
    # breaking do_package's "host contamination" ownership check.
    install -d ${D}${datadir}/cef
    cp -r ${S}/. ${D}${datadir}/cef/
    # Remove ANGLE/SwiftShader on GPU-less targets (software OSR only) to avoid a
    # GPU-process crash loop.
    rm -f ${D}${datadir}/cef/Release/libEGL.so \
          ${D}${datadir}/cef/Release/libGLESv2.so \
          ${D}${datadir}/cef/Release/vk_swiftshader_icd.json

    # Dev: wrapper static lib (this recipe's own do_compile build, reused as-is
    # by libchromium-nc_git.bb via CEF_WRAPPER_LIB) + a conventional
    # ${includedir}/cef copy for any consumer that does normal -I/find_package
    # discovery instead of add_subdirectory(CEF_ROOT).
    install -d ${D}${libdir} ${D}${includedir}/cef
    install -m 0644 ${B}/libcef_dll_wrapper/libcef_dll_wrapper.a ${D}${libdir}/
    cp -r ${S}/include/. ${D}${includedir}/cef/
    install -m 0755 ${S}/Release/libcef.so ${D}${libdir}/libcef.so
}

FILES:${PN} = "${datadir}/cef ${libdir}/libcef.so"
FILES:${PN}-dev = "${libdir}/libcef_dll_wrapper.a ${includedir}/cef"

# libdir: libcef.so/libvulkan.so.1/libvk_swiftshader.so are intentionally
# shipped under ${datadir}/cef/Release (not ${libdir}) -- that's the exact
# CEF_ROOT/Release layout the webview_flutter_view plugin's CMakeLists.txt
# hardcodes (target_link_directories(... ${CEF_ROOT}/${CEF_BUILD_TYPE})); the
# ${libdir}/libcef.so copy above is the separate, standard-location one QA
# actually wants for ldconfig/runtime linking.
INSANE_SKIP:${PN} += "already-stripped ldflags dev-so libdir"
INSANE_SKIP:${PN}-dbg += "libdir"

# libcef.so (a huge prebuilt Chromium binary) links directly against these
# system libraries; shlibs auto-detection didn't resolve them all, so RDEPENDS
# explicitly on the packages that provide each soname.
RDEPENDS:${PN} += "\
    libxrandr \
    libgbm \
    libdrm \
    libxcb \
    libxkbcommon \
    pango \
    cairo \
    alsa-lib \
    at-spi2-core \
    cups-lib \
    dbus-lib \
    glib-2.0 \
    nspr \
    nss \
    libx11 \
    libxcomposite \
    libxdamage \
    libxext \
    libxfixes \
"

# The homescreen re-execs with LD_PRELOAD=libcef.so (static TLS requirement);
# flutter-auto bakes HOMESCREEN_CEF_PRELOAD_PATH when the webview plugin is ON.
