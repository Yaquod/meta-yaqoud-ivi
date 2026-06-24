FILESEXTRAPATHS:prepend := "${THISDIR}/weston-init/files:"

SRC_URI:append = " file://weston-socket.conf"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}/weston.service.d/
    install -m 0644 ${WORKDIR}/weston-socket.conf \
        ${D}${systemd_system_unitdir}/weston.service.d/socket.conf
}

FILES:${PN} += "${systemd_system_unitdir}/weston.service.d/socket.conf"
