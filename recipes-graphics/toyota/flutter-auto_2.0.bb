#
# Copyright (c) 2020-2025 Joel Winarske. All rights reserved.
#
# SELF-CONTAINED build recipe for ivi-homescreen (flutter-auto v2.0) + our plugins
# fork, wired for the flutter_ivi app (in-scene CEF webview via chromium_dart_view).
#
# See flutter-ivi/docs/BUILD_IVI_HOMESCREEN.md for the full handoff notes (this
# recipe is adapted from yocto/recipes-graphics/toyota/flutter-auto_2.0.bb there).
# Overrides/replaces meta-flutter's own flutter-auto_2.0.bb outright (same PN/PV,
# higher layer priority) -- not a bbappend, per that doc's own guidance.
#
# BOTH the shell and the plugins are FORKS with real commits on top:
#   * SHELL   — AhmedAdelWafdy7/ivi-homescreen v2.0 @ 2751a0fb, 4 commits ahead of
#               toyota-connected/v2.0 (ff677e13): wires CEF into main.cc + CMake
#               (CEF preload path, webview_flutter_view dep) and bumps the
#               wayland-cxx-scanner submodule. Confirmed pushed
#               (git ls-remote .../ivi-homescreen v2.0 -> 2751a0fb).
#   * PLUGINS — AhmedAdelWafdy7/ivi-homescreen-plugins carries chromium_dart_view.
#
# CEF: the source doc uses depot_tools-native/gclient (fetched at build time by
# meta-flutter's own webview_flutter_view build). That's not viable in a
# network-sandboxed Yocto do_compile, so this recipe uses our own pinned prebuilt
# CEF dist instead (recipes-graphics/chromium-dart/cef_128.bb) -- same CEF_ROOT
# contract (add_subdirectory(${CEF_ROOT}) builds libcef_dll_wrapper.a in-tree),
# same dist libchromium-nc_git.bb consumes, just sourced differently.
#

SUMMARY = "Toyota IVI Homescreen v2.0 (flutter_ivi CEF build)"
DESCRIPTION = "Toyota's Flutter Embedder that communicates with AGL-compositor/Wayland compositors"
AUTHOR = "joel.winarske@toyotaconnected.com"
HOMEPAGE = "https://github.com/toyota-connected/ivi-homescreen"
BUGTRACKER = "https://github.com/toyota-connected/ivi-homescreen/issues"
SECTION = "graphics"

LICENSE = "Apache-2.0 & Apache-2.0"
LIC_FILES_CHKSUM = "\
    file://LICENSE;md5=39ae29158ce710399736340c60147314 \
    file://${S}/ivi-homescreen-plugins/LICENSE;md5=39ae29158ce710399736340c60147314 \
    "

DEPENDS += "\
    glib-2.0 \
    libxkbcommon \
    wayland \
    wayland-native \
    wayland-protocols \
    compiler-rt \
    libcxx \
    lld-native \
    cef \
    wayland-cxx-scanner-native \
"

REQUIRED_DISTRO_FEATURES = "wayland"

# ── Pinned sources ────────────────────────────────────────────────────────────
# SHELL: AhmedAdelWafdy7/ivi-homescreen v2.0 @ f151d973. This fork's 4 local
# commits (CEF wiring in main.cc/CMakeLists.txt + wayland-cxx-scanner submodule
# bump to 9c86b8e7) were originally cut from the OLD upstream base ff677e13,
# which predates ivi-homescreen's spdlog-to-ihs_shared logging rewrite
# (shell/logging/logging.h's `namespace ihs::log`, IHS_DEBUG/IHS_TRACE) --
# but the plugins fork below (based on upstream d55ab69) already requires
# that rewrite (plugins/common/* and nav_render_view call ihs::log:: directly,
# "use of undeclared identifier 'ihs'" without it). Rebased those 4 commits
# (plus our own wlcxxgen_* dependency-list completion, see below) onto
# toyota-connected/ivi-homescreen v2.0 HEAD (ed234cec, which has the
# ihs_shared rewrite) to fix the mismatch at its root instead of shimming the
# plugins side. One real conflict in shell/main.cc: dropped a leftover
# `gLogger = std::make_unique<Logging>()` line the old base's CEF-wiring
# commit added -- that class/global doesn't exist anywhere in the tree past
# the ihs_shared rewrite (logging now goes through IHS_LOGGING_START, already
# called on the next line); kept the actual CEF LD_PRELOAD re-exec block.
#
# f151d973 (on top of the rebase) completes the post-add_subdirectory(shell)
# wlcxxgen_* dependency list in the top-level CMakeLists.txt -- it only wired
# 4 of the ~9 generated wayland-protocol headers against plugin targets;
# chromium_dart_view needs input_timestamps_client.hpp too.
#
# Fetched gitsm, so the bumped submodule (jwinarske/wayland-cxx-scanner @
# 9c86b8e7, on origin/main) is pulled automatically.
HOMESCREEN_URL    ??= "github.com/AhmedAdelWafdy7/ivi-homescreen.git"
HOMESCREEN_BRANCH ??= "v2.0"
# 05b409bb: adds CEF_INSTALL_ROOT (see EXTRA_OECMAKE below) so
# HOMESCREEN_CEF_PRELOAD_PATH bakes the on-target CEF path, not the
# build-host sysroot path CEF_ROOT has to be for this staged-CEF build.
HOMESCREEN_COMMIT ??= "05b409bb54c40098275163ea4c8c5686d8ff0309"

# PLUGINS: our fork carrying plugins/chromium_dart_view + its registration.
PLUGINS_URL       ??= "github.com/AhmedAdelWafdy7/ivi-homescreen-plugins.git"
PLUGINS_BRANCH    ??= "v2.0"
# Structural Yocto-specific fixes only (the ihs::log mismatch is now fixed at
# the shell level above, not shimmed here):
# 4394faa: add_subdirectory(CEF_ROOT) needs an explicit binary dir since
# CEF_ROOT is a Yocto sysroot path here (cef_128.bb), not an in-tree
# depot_tools dist like the reference native build uses.
# cfa0e43: CEF's own generated wrapper code trips this project's global
# -Wsign-conversion -Werror; disabled just for libcef_dll_wrapper.
# b7414fc: shell/wayland/*.h pull in wayland-cxx-scanner generated headers
# that were never on plugins' include path.
# 93bcd86: wayland_client.hpp needs the wl/ header-only framework
# (wayland-cxx::wayland-cxx), linked generically for every plugin.
# 3a0a2d7: CMP0079 NEW needed locally for the cross-scope target_link_libraries
# in the previous commit.
# 9e5d422: libcef_dll_wrapper silently fell back to libstdc++ (CEF's own
# cmake never sets -stdlib on Linux), breaking final link against the rest of
# this libc++ project; forced target-scoped.
# a6c1fb6: webview_flutter_view_plugin.cc also baked CEF_ROOT (build-time
# sysroot path) directly for its own runtime cache/Resources paths; now uses
# CEF_INSTALL_ROOT (see HOMESCREEN_COMMIT above) like the shell does.
PLUGINS_COMMIT    ??= "a6c1fb64aaa3783cd5172609a5d2b5e778593045"

SRC_URI = "\
    gitsm://${HOMESCREEN_URL};protocol=https;branch=${HOMESCREEN_BRANCH};name=homescreen \
    gitsm://${PLUGINS_URL};protocol=https;branch=${PLUGINS_BRANCH};name=plugins;destsuffix=${S}/ivi-homescreen-plugins \
"
SRCREV_FORMAT .= "_homescreen"
SRCREV_homescreen = "${HOMESCREEN_COMMIT}"
SRCREV_FORMAT .= "_plugins"
SRCREV_plugins = "${PLUGINS_COMMIT}"

CRASH_HANDLER_DSN ??= ""

# ── One C++ runtime everywhere = libc++ (must match libchromium-nc + CEF) ──────
TOOLCHAIN = "clang"
TOOLCHAIN_NATIVE = "clang"
TC_CXX_RUNTIME = "llvm"
PREFERRED_PROVIDER_llvm = "clang"
PREFERRED_PROVIDER_llvm-native = "clang-native"
PREFERRED_PROVIDER_libgcc = "compiler-rt"
LIBCPLUSPLUS = "-stdlib=libc++"

inherit cmake features_check pkgconfig

PACKAGECONFIG ??= "\
    backend-wayland-egl \
    egl-3d \
    egl-transparency \
    egl-multisample \
    \
    client-xdg \
    client-agl-shell \
    \
    ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'wdt_systemd', '', d)} \
    \
    nav_render_view \
    webview_flutter_view \
    chromium_dart_view \
    \
    audioplayer_linux \
    go_router \
    secure-storage \
    url_launcher \
    desktop_window_linux \
    rive-text \
    "

PACKAGECONFIG[backend-wayland-drm] = "-DBUILD_BACKEND_WAYLAND_DRM=ON,-DBUILD_BACKEND_WAYLAND_DRM=OFF"
PACKAGECONFIG[backend-wayland-egl] = "-DBUILD_BACKEND_WAYLAND_EGL=ON,-DBUILD_BACKEND_WAYLAND_EGL=OFF,virtual/egl"
PACKAGECONFIG[backend-wayland-vulkan] = "-DBUILD_BACKEND_WAYLAND_VULKAN=ON,-DBUILD_BACKEND_WAYLAND_VULKAN=OFF,vulkan-loader"

PACKAGECONFIG[client-xdg] = "-DENABLE_XDG_CLIENT=ON,-DENABLE_XDG_CLIENT=OFF"
PACKAGECONFIG[client-agl-shell] = "-DENABLE_AGL_SHELL_CLIENT=ON,-DENABLE_AGL_SHELL_CLIENT=OFF"
PACKAGECONFIG[client-ivi-shell] = "-DENABLE_IVI_SHELL_CLIENT=ON,-DENABLE_IVI_SHELL_CLIENT=OFF"

PACKAGECONFIG[egl-3d] = "-DBUILD_EGL_ENABLE_3D=ON, -DBUILD_EGL_ENABLE_3D=OFF"
PACKAGECONFIG[egl-transparency] = "-DBUILD_EGL_TRANSPARENCY=ON,-DBUILD_EGL_TRANSPARENCY=OFF"
PACKAGECONFIG[egl-multisample] = "-DBUILD_EGL_ENABLE_MULTISAMPLE=ON,-DBUILD_EGL_ENABLE_MULTISAMPLE=OFF"

PACKAGECONFIG[wdt] = "-DBUILD_WATCHDOG=ON, -DBUILD_WATCHDOG=OFF"
PACKAGECONFIG[wdt_systemd] = "-DBUILD_WATCHDOG=ON -DBUILD_SYSTEMD_WATCHDOG=ON, -DBUILD_SYSTEMD_WATCHDOG=OFF, systemd"

PACKAGECONFIG[disable-plugins] = "-DDISABLE_PLUGINS=ON"

PACKAGECONFIG[filament-view] = "\
    -DBUILD_PLUGIN_FILAMENT_VIEW=ON \
    -DFILAMENT_INCLUDE_DIR=${STAGING_INCDIR}/filament \
    -DFILAMENT_LINK_LIBRARIES_DIR=${STAGING_LIBDIR}/filament, \
    -DBUILD_PLUGIN_FILAMENT_VIEW=OFF, filament-vk curl vulkan-loader"
PACKAGECONFIG[layer-playground-view] = "\
    -DBUILD_PLUGIN_LAYER_PLAYGROUND_VIEW=ON, \
    -DBUILD_PLUGIN_LAYER_PLAYGROUND_VIEW=OFF"
PACKAGECONFIG[nav_render_view] = "\
    -DBUILD_PLUGIN_NAV_RENDER_VIEW=ON, \
    -DBUILD_PLUGIN_NAV_RENDER_VIEW=OFF,,"
PACKAGECONFIG[webview_flutter_view] = "\
    -DBUILD_PLUGIN_WEBVIEW_FLUTTER_VIEW=ON, \
    -DBUILD_PLUGIN_WEBVIEW_FLUTTER_VIEW=OFF"

# Our in-scene CEF platform-view bridge (lives in the plugins fork).
PACKAGECONFIG[chromium_dart_view] = "\
    -DBUILD_PLUGIN_CHROMIUM_DART_VIEW=ON, \
    -DBUILD_PLUGIN_CHROMIUM_DART_VIEW=OFF"

PACKAGECONFIG[audioplayer_linux] = "\
    -DBUILD_PLUGIN_AUDIOPLAYERS_LINUX=ON, \
    -DBUILD_PLUGIN_AUDIOPLAYERS_LINUX=OFF, \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    "
PACKAGECONFIG[url_launcher] = "-DBUILD_PLUGIN_URL_LAUNCHER=ON,-DBUILD_PLUGIN_URL_LAUNCHER=OFF"
PACKAGECONFIG[secure-storage] = "-DBUILD_PLUGIN_SECURE_STORAGE=ON,-DBUILD_PLUGIN_SECURE_STORAGE=OFF, libsecret"
PACKAGECONFIG[file_selector] = "-DBUILD_PLUGIN_FILE_SELECTOR=ON,-DBUILD_PLUGIN_FILE_SELECTOR=OFF, zenity"
PACKAGECONFIG[cloud_firestore] = "\
    -DBUILD_PLUGIN_CLOUD_FIRESTORE=ON \
    -DFIREBASE_CPP_SDK_DIR=${STAGING_INCDIR}/firebase-cpp-sdk \
    -DFIREBASE_SDK_LIBDIR=${STAGING_LIBDIR}/firebase-cpp-sdk, \
    -DBUILD_PLUGIN_CLOUD_FIRESTORE=OFF \
    "
PACKAGECONFIG[firebase_auth] = "\
    -DBUILD_PLUGIN_FIREBASE_AUTH=ON \
    -DFIREBASE_CPP_SDK_DIR=${STAGING_INCDIR}/firebase-cpp-sdk \
    -DFIREBASE_SDK_LIBDIR=${STAGING_LIBDIR}/firebase-cpp-sdk, \
    -DBUILD_PLUGIN_FIREBASE_AUTH=OFF, \
    libsecret \
"
PACKAGECONFIG[firebase_storage] = "\
    -DBUILD_PLUGIN_FIREBASE_STORAGE=ON \
    -DFIREBASE_CPP_SDK_DIR=${STAGING_INCDIR}/firebase-cpp-sdk \
    -DFIREBASE_SDK_LIBDIR=${STAGING_LIBDIR}/firebase-cpp-sdk, \
    -DBUILD_PLUGIN_FIREBASE_STORAGE=OFF"
PACKAGECONFIG[desktop_window_linux] = "-DBUILD_PLUGIN_DESKTOP_WINDOW_LINUX=ON,-DBUILD_PLUGIN_DESKTOP_WINDOW_LINUX=OFF"
PACKAGECONFIG[go_router] = "-DBUILD_PLUGIN_GO_ROUTER=ON,-DBUILD_PLUGIN_GO_ROUTER=OFF"
PACKAGECONFIG[google_sign_in] = "-DBUILD_PLUGIN_GOOGLE_SIGN_IN=ON,-DBUILD_PLUGIN_GOOGLE_SIGN_IN=OFF, curl"
PACKAGECONFIG[pdf] = "-DBUILD_PLUGIN_PDF=ON, -DBUILD_PLUGIN_PDF=OFF, pdfium"
PACKAGECONFIG[flatpak] = "-DBUILD_PLUGIN_FLATPAK=ON, -DBUILD_PLUGIN_FLATPAK=OFF, flatpak"
PACKAGECONFIG[camera] = "-DBUILD_PLUGIN_CAMERA=ON, -DBUILD_PLUGIN_CAMERA=OFF, libcamera"
PACKAGECONFIG[camera-pipewire] = "-DBUILD_PLUGIN_CAMERA_PIPEWIRE=ON -DBUILD_PLUGIN_CAMERA=OFF, \
    -DBUILD_PLUGIN_CAMERA_PIPEWIRE=OFF, libcamera pipewire"
PACKAGECONFIG[video-player] = "-DBUILD_PLUGIN_VIDEO_PLAYER_LINUX=ON,-DBUILD_PLUGIN_VIDEO_PLAYER_LINUX=OFF, \
    ffmpeg \
    gstreamer1.0 \
    gstreamer1.0-libav \
    gstreamer1.0-plugins-base"
PACKAGECONFIG[rive-text] = "-DBUILD_PLUGIN_RIVE_TEXT=ON, -DBUILD_PLUGIN_RIVE_TEXT=OFF, rive-text"

PACKAGECONFIG[sentry] = "\
    -DBUILD_CRASH_HANDLER=ON \
    -DSENTRY_NATIVE_LIBDIR=${STAGING_LIBDIR} \
    -DCRASH_HANDLER_DSN=${CRASH_HANDLER_DSN}, \
    -DBUILD_CRASH_HANDLER=OFF, sentry libunwind"
PACKAGECONFIG[dlt] = "-DENABLE_DLT=ON, -DENABLE_DLT=OFF"
PACKAGECONFIG[sanitize] = "-DSANITIZE_ADDRESS=ON, -DSANITIZE_ADDRESS=OFF"

PACKAGECONFIG[examples] = "-DBUILD_EXAMPLES=ON, -DBUILD_EXAMPLES=OFF"
PACKAGECONFIG[verbose] = "-DCMAKE_BUILD_TYPE=Debug -DDEBUG_PLATFORM_MESSAGES=ON, -DDEBUG_PLATFORM_MESSAGES=OFF"

# ── CEF at build time (shared with libchromium-nc) ────────────────────────────
# -DPROJECT_ARCH=arm64: webview_flutter_view's add_subdirectory(${CEF_ROOT}) reruns
# CEF's own cmake/cef_variables.cmake here -- without this it falls back to
# CMAKE_HOST_SYSTEM_PROCESSOR (the x86_64 build host) and hardcodes -march=x86-64,
# which the aarch64 cross clang rejects. Same fix as cef_128.bb's own EXTRA_OECMAKE
# (that one only covers cef_128.bb's own standalone libcef_dll_wrapper.a build;
# this is a second, separate in-tree CEF build the plugin does itself).
# NOTE: -DCEF_RUNTIME_LIBRARY_FLAG=-stdlib=libc++ does NOT work here -- checked
# cef_variables.cmake directly: that variable defaults to "/MT" and is used
# only inside a Windows/MSVC-only block, a no-op on Linux/Clang. The actual
# libstdc++-vs-libc++ ABI mismatch this caused (undefined
# std::_Rb_tree_*/std::__throw_length_error at final link, since CEF's vendored
# cmake resets CXX flags for its own subtree and never sets -stdlib on Linux)
# is fixed target-scoped instead, in the plugins fork's
# webview_flutter_view/CMakeLists.txt (target_compile_options(libcef_dll_wrapper
# PRIVATE ... -stdlib=libc++), alongside its existing -Wno-error overrides).
EXTRA_OECMAKE += "\
    -D LLVM_CONFIG=${STAGING_BINDIR_NATIVE}/llvm-config \
    -D PLUGINS_DIR=${S}/ivi-homescreen-plugins/plugins \
    -D ENABLE_STATIC_LINK=OFF \
    -D ENABLE_LTO=ON \
    -D EXE_OUTPUT_NAME=${PN} \
    -D BUILD_UNIT_TESTS=OFF \
    -D BUILD_DOCS=OFF \
    -D CEF_ROOT=${STAGING_DIR_TARGET}${datadir}/cef \
    -D CEF_BUILD_TYPE=Release \
    -D PROJECT_ARCH=arm64 \
    -D CEF_INSTALL_ROOT=${datadir}/cef \
"

RDEPENDS:${PN} += "\
   flutter-engine \
    ${@bb.utils.contains('PACKAGECONFIG', 'flatpak', 'flatpak', '', d)} \
    ${@bb.utils.contains('PACKAGECONFIG', 'rive-text', 'rive-text', '', d)} \
   "

INSANE_SKIP:${PN}-dbg += " buildpaths"

BBCLASSEXTEND = "verbose-logs"
