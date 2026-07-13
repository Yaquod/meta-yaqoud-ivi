SUMMARY = "wayland-cxx-scanner -- C++17 Wayland protocol binding generator (host tool)"
DESCRIPTION = "Codegen tool that generates type-safe C++17 Wayland protocol \
bindings. ivi-homescreen (flutter-auto) vendors this as a submodule \
(third_party/wayland-cxx-scanner) for its own build; libchromium-nc and the \
flutter-auto chromium_dart_view plugin build also need it as a native codegen \
tool -- this recipe provides the missing wayland-cxx-scanner-native dependency \
(see Yaquod/flutter-ivi docs/YOCTO.md, 'integrator' item #4)."
HOMEPAGE = "https://github.com/jwinarske/wayland-cxx-scanner"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8992d37861cd7e48b171be43673f1d7f"

SRCREV = "9c86b8e7e87e2c9d5ff34939d044df4053e70970"
SRC_URI = "git://github.com/jwinarske/wayland-cxx-scanner.git;protocol=https;branch=main"

DEPENDS = "pugixml-native"

inherit cmake

# We only ever want the -native variant (nothing DEPENDS on the plain target
# recipe, so it's simply never built).
BBCLASSEXTEND = "native"

# Native (non-cross) build: WAYLAND_CXX_SCANNER_BUILD_TOOL defaults ON already
# (cmake/options.cmake), TOOL_ONLY skips the header-only framework/package/tests
# install -- we only want the codegen executable staged for other -native/target
# recipes to invoke via -DWAYLAND_CXX_SCANNER_EXECUTABLE=.
EXTRA_OECMAKE = "\
    -DWAYLAND_CXX_SCANNER_TOOL_ONLY=ON \
    -DWAYLAND_CXX_SCANNER_BUILD_TESTS=OFF \
    -DWAYLAND_CXX_SCANNER_BUILD_EXAMPLES=OFF \
    -DWAYLAND_CXX_WERROR=OFF \
"

# chromium_dart/native/CMakeLists.txt (cloned + read directly to confirm) does
# NOT find_package(wayland-cxx-scanner) -- it raw-includes
# ${WAYLAND_CXX_DIR}/cmake/WaylandCxxScanner.cmake and adds
# ${WAYLAND_CXX_DIR}/include as an include dir, i.e. it wants a
# checkout-shaped tree (cmake/ + include/ at the same root), not TOOL_ONLY's
# installed package. Stage those two directories verbatim from source under
# ${datadir}/wayland-cxx-scanner so -DWAYLAND_CXX_DIR=${STAGING_DATADIR_NATIVE}/wayland-cxx-scanner
# (as libchromium-nc_git.bb already passes) resolves correctly.
do_install:append() {
    install -d ${D}${datadir}/wayland-cxx-scanner
    cp -a ${S}/cmake ${D}${datadir}/wayland-cxx-scanner/
    cp -a ${S}/include ${D}${datadir}/wayland-cxx-scanner/
}

FILES:${PN} += "${datadir}/wayland-cxx-scanner"
