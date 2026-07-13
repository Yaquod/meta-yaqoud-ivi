SUMMARY = "A Flutter IVI application"
DESCRIPTION = "Flutter IVI: navigation (Google Maps in a CEF webview), Spotify \
Connect, live vehicle telemetry over Zenoh from autoware-agent, and trip \
dispatch over gRPC from iv-cloud-gateway."
AUTHOR = "Ahmed Wafdy"
HOMEPAGE = "https://github.com/Yaquod/flutter-ivi"
BUGTRACKER = "https://github.com/Yaquod/flutter-ivi/issues"
SECTION = "graphics"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRCREV = "352492c10f03ba353c3b25801c0211f7fba044b8"
SRC_URI = "git://github.com/Yaquod/flutter-ivi.git;lfs=0;branch=main;protocol=https \
           file://env.sample \
           file://env.real \
           "

PUBSPEC_APPNAME = "flutter_ivi"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "yaqoud-flutter-ivi"
PUBSPEC_IGNORE_LOCKFILE = "1"

inherit flutter-app

# -- App config (.env) is a BUNDLED ASSET, baked in at `flutter build` time ----
# flutter_dotenv reads assets/.env; it is gitignored in the app repo, so provide
# it here BEFORE the build. files/env.real (gitignored in *this* layer too, see
# .gitignore) carries the real target values when present; otherwise fall back
# to the placeholder env.sample and warn loudly.
do_configure:prepend() {
    if [ -f ${UNPACKDIR}/env.real ]; then
        install -m 0600 ${UNPACKDIR}/env.real ${S}/.env
    else
        install -m 0600 ${UNPACKDIR}/env.sample ${S}/.env
        bbwarn "flutter-ivi: using env.sample placeholders for .env -- add recipes-graphics/flutter-ivi/files/env.real with real target values (GOOGLE_MAPS_API_KEY, GATEWAY_HOST, ZENOH_VEHICLE_IP, VIN_NUMBER, ...)"
    fi
}

# meta-flutter's own do_install() (conf/include/flutter-app.inc) copies the
# built flutter_assets via a plain shell glob --
# `cp -r .../flutter_assets/* ...` -- which silently excludes dotfiles.
# `flutter build` itself correctly bundles .env into build/flutter_assets/.env
# (pubspec.yaml declares it under assets:), but that glob then drops it before
# it reaches the packaged image, so flutter_dotenv's load() fails at runtime
# ("Environment file '.env' not found"). Copy it explicitly instead of
# patching the external meta-flutter layer.
do_install:append() {
    for mode in ${FLUTTER_APP_RUNTIME_MODES}; do
        if [ -f ${S}/${FLUTTER_APPLICATION_PATH}/build/flutter_assets/.env ]; then
            # 0600 (root-only) would be unreadable by the actual runtime user:
            # flutter-auto-ivi-app.service runs as User=agl-driver, not root
            # (confirmed on-device: manually placing a 0600 root-owned .env at
            # this exact path still hit "Environment file '.env' not found",
            # since flutter_dotenv's rootBundle-based load() couldn't open it
            # -- same class of bug as iv-cloud-gateway's .env before its
            # chown fix). 0644 matches the rest of this bundle's default
            # (world-readable) permissions; nothing here is more sensitive
            # than what any local process on this single-purpose device can
            # already reach.
            install -m 0644 ${S}/${FLUTTER_APPLICATION_PATH}/build/flutter_assets/.env \
                ${D}${FLUTTER_INSTALL_DIR}/${FLUTTER_SDK_VERSION}/${mode}/data/flutter_assets/.env
        fi
    done
}

# -- Runtime dependencies (loaded by the ivi-homescreen embedder) --------------
#   flutter-auto   -- embedder, built WITH chromium_dart_view + webview plugins
#                     (see ../toyota/flutter-auto_2.0.bbappend).
#   libchromium-nc -- CEF/FFI backend the webview dlopen()s (CHROMIUM_DART_LIB).
#   zenoh-c        -- libzenohc.so (autoware-agent telemetry); already provided
#                     by recipes-connectivity/zenoh-c in this layer.
#   spotifyd + avahi-daemon -- open Spotify Connect device over mDNS.
#   ca-certificates -- TLS to Google Maps / Spotify / the fleet gateway.
#
# NOTE: unlike upstream's own yocto/ recipe, this does NOT `inherit systemd` /
# install its own flutter-ivi.service -- this layer already has a tailored
# recipes-graphics/flutter-auto/files/flutter-auto-ivi-app.service (agl-driver
# user, versioned bundle path, --output-index, agl-compositor ordering) that
# launches this exact bundle; running both would race two flutter-auto
# instances against the same output. That service now also carries
# CHROMIUM_DART_LIB / VIDEO_PLAYER_AUDIO_SINK for this app's webview backend.
RDEPENDS:${PN} += "\
    flutter-auto \
    libchromium-nc \
    zenoh-c \
    spotifyd \
    avahi-daemon \
    ca-certificates \
    "