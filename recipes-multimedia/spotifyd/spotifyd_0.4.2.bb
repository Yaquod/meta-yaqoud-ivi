SUMMARY = "spotifyd -- a Spotify Connect daemon"
DESCRIPTION = "Advertises the IVI as an open Spotify Connect device. \
Zeroconf/multi-user: any rider's Spotify app on the vehicle LAN can connect \
with their own account -- no per-device account provisioning."
HOMEPAGE = "https://github.com/Spotifyd/spotifyd"
SECTION = "multimedia"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=84dcc94da3adb52b53ae4fa38fe49e5d"

SRCREV = "c5b94367014856a8c541dea565cbd332e034fb9e"
# spotifyd's default branch is "master", not "main".
SRC_URI = "git://github.com/Spotifyd/spotifyd.git;protocol=https;branch=master"

# openssl: librespot's TLS stack (openssl-sys) needs the target dev headers/libs,
# located via pkg-config below.
DEPENDS = "alsa-lib pulseaudio openssl"
inherit cargo pkgconfig

# spotifyd/librespot pull in 100+ transitive crates and this recipe carries no
# crate:// SRC_URI entries to vendor them, so cargo.bbclass's default --frozen
# build (which requires every crate already staged in cargo_home/bitbake) dies
# on the first one it needs ("no matching package named `alsa` found"). Same
# situation as recipes-connectivity/zenoh-c/zenoh-c_1.9.0.bb (also a plain
# `inherit cargo` recipe with zero crate:// entries) -- mirror its fix here:
# let this task reach the real crates.io registry over the network instead of
# bitbake's empty local vendor mirror.
do_compile[network] = "1"
CARGO_DISABLE_BITBAKE_VENDORING = "1"

# Pick the audio backend(s) to compile in. Target usually alsa; match
# SPOTIFYD_BACKEND in the app .env (recipes-graphics/flutter-ivi/files/env.*).
#
# Override CARGO_BUILD_FLAGS rather than do_compile itself: an earlier attempt
# at a from-scratch `cargo build ...` do_compile() dropped the RUSTFLAGS export
# that oe_cargo_build() normally sets up, which broke resolving std/core for
# the custom aarch64-poky-linux-gnu target spec ("can't find crate for
# `std`"). Keeping cargo_do_compile/oe_cargo_build (and therefore also
# cargo_do_install's matching ${CARGO_TARGET_SUBDIR} path -- no do_install
# override needed either) and just stripping the one flag that breaks
# non-vendored builds (--frozen, see CARGO_DISABLE_BITBAKE_VENDORING above) is
# the minimal change; this is cargo.bbclass's own CARGO_BUILD_FLAGS definition
# verbatim minus --frozen, plus our feature selection appended.
CARGO_BUILD_FLAGS = "-v --target ${RUST_HOST_SYS} ${BUILD_MODE} --manifest-path=${CARGO_MANIFEST_PATH} --no-default-features --features pulseaudio_backend,alsa_backend"

FILES:${PN} = "${bindir}/spotifyd"

# OPEN, MULTI-USER (zeroconf) Connect sink -- needs a working mDNS stack on the
# target: avahi-daemon running, UDP 5353 open, multicast allowed on the vehicle
# net. flutter-ivi.bb already RDEPENDS avahi-daemon alongside this.
RDEPENDS:${PN} += "avahi-daemon pipewire-alsa"
