FILESEXTRAPATHS:prepend := "${THISDIR}/weston-init/files:"

WESTONPASSWD = "\$5\$oQPFyE/HPxXfwjeM\$WRFaZiqrzltNTt9szaHw9i4NgnfrL6LeBTw4oQGI0k5"

USERADD_PARAM:${PN} = "--home /home/weston \
    --shell /bin/sh \
    --user-group \
    -G video,input,render,wayland,tty \
    -p '${WESTONPASSWD}' weston"

SRC_URI:append = " \
    file://weston.ini \
    file://weston-runtime-dir.conf \
    file://weston.service \
    file://weston-socket.conf \
"

WESTON_MAJOR = "${@d.getVar('PV').split('.')[0]}"

do_install:append() {
    # VNC TLS directory
    install -m 0755 -d ${D}${sysconfdir}/vnc/keys/

    # weston.ini
    install -m 0755 -d ${D}${sysconfdir}/xdg/weston/
    install -m 0644 ${WORKDIR}/weston.ini \
        ${D}${sysconfdir}/xdg/weston/weston.ini

    # tmpfiles (creates /run/user/1000 at boot)
    install -m 0755 -d ${D}${libdir}/tmpfiles.d/
    install -m 0644 ${WORKDIR}/weston-runtime-dir.conf \
        ${D}${libdir}/tmpfiles.d/weston-runtime-dir.conf

    # weston.service
    install -m 0644 ${WORKDIR}/weston.service \
        ${D}${systemd_system_unitdir}/weston.service

    # weston socket override
    install -d ${D}${systemd_system_unitdir}/weston.service.d/
    install -m 0644 ${WORKDIR}/weston-socket.conf \
        ${D}${systemd_system_unitdir}/weston.service.d/socket.conf
}

SYSTEMD_SERVICE:${PN} += "weston.service"

FILES:${PN} += " \
    ${sysconfdir}/xdg/weston/weston.ini \
    ${sysconfdir}/vnc/keys \
    ${libdir}/tmpfiles.d/weston-runtime-dir.conf \
    ${systemd_system_unitdir}/weston.service.d/socket.conf \
"
