SUMMARY = "Systemd service to launch the VehicleAutowareAgent Docker container at boot"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://autoware-agent.service"

S = "${UNPACKDIR}"

inherit systemd allarch

do_install() {
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/autoware-agent.service ${D}${systemd_unitdir}/system/autoware-agent.service
}

FILES:${PN} = "${systemd_unitdir}/system/autoware-agent.service"
SYSTEMD_SERVICE:${PN} = "autoware-agent.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
