SUMMARY = "Persist the system clock across reboots on boards without a reliable RTC"
DESCRIPTION = "Saves the system time on shutdown and periodically, and restores it \
as a best-effort estimate at early boot, before network time sync is available. \
Standalone reimplementation of Debian's fake-hwclock for OE/systemd, since no \
upstream OE recipe exists for it."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit systemd

SRC_URI = " \
    file://fake-hwclock \
    file://fake-hwclock.service \
    file://fake-hwclock-save.service \
    file://fake-hwclock-save.timer \
"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${UNPACKDIR}/fake-hwclock ${D}${sbindir}/fake-hwclock

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/fake-hwclock.service ${D}${systemd_system_unitdir}/fake-hwclock.service
    install -m 0644 ${UNPACKDIR}/fake-hwclock-save.service ${D}${systemd_system_unitdir}/fake-hwclock-save.service
    install -m 0644 ${UNPACKDIR}/fake-hwclock-save.timer ${D}${systemd_system_unitdir}/fake-hwclock-save.timer
}

FILES:${PN} += "${systemd_system_unitdir}"

SYSTEMD_SERVICE:${PN} = "fake-hwclock.service fake-hwclock-save.timer"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
