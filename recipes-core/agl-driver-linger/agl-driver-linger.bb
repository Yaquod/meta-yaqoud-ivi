SUMMARY = "Enable systemd-logind lingering for agl-driver"
DESCRIPTION = "Ensures /run/user/<uid> and the D-Bus user session for \
agl-driver survive independently of any interactive login session, \
so agl-compositor and flutter-auto services keep a stable runtime dir \
across restarts and reboots."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit allarch

do_install() {
    install -d ${D}/var/lib/systemd/linger
    touch ${D}/var/lib/systemd/linger/agl-driver
}

FILES:${PN} += "/var/lib/systemd/linger/agl-driver"
