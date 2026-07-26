SUMMARY = "Lock Jetson clocks to their maximum at every boot"
DESCRIPTION = "meta-tegra ships the jetson_clocks binary (tegra-tools-jetson-clocks) \
but no systemd unit to actually run it at boot -- nvpmodel.service alone only \
selects a power *mode* (see tegra-nvpmodel-base_%.bbappend for the MAXN_SUPER \
default), it doesn't lock clocks to that mode's maximum. Without this, clocks \
still idle/scale down dynamically, which was enough on its own to make \
Autoware Universe's trajectory planner intermittently miss its 1s real-time \
deadline during an actual drive, triggering repeated MRM emergency-stops -- \
confirmed on-device."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRC_URI = "file://jetson-clocks.service"

inherit systemd allarch

SYSTEMD_SERVICE:${PN} = "jetson-clocks.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "tegra-tools-jetson-clocks"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/jetson-clocks.service ${D}${systemd_system_unitdir}/jetson-clocks.service
}

FILES:${PN} = "${systemd_system_unitdir}/jetson-clocks.service"
