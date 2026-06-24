SUMMARY = "Flutter Auto IVI app systemd service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://flutter-auto-app.service"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "flutter-auto-app.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/flutter-auto-app.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} += "${systemd_system_unitdir}/flutter-auto-app.service"
