# Minimal bbappend - agl-compositor-init handles the full compositor setup
# Only keep socket name override for compatibility
FILESEXTRAPATHS:prepend := "${THISDIR}/weston-init/files:"

SRC_URI:append = " file://weston-socket.conf"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}/weston.service.d/
    install -m 0644 ${UNPACKDIR}/weston-socket.conf \
        ${D}${systemd_system_unitdir}/weston.service.d/socket.conf
}

FILES:${PN} += "${systemd_system_unitdir}/weston.service.d/socket.conf"

# Disable weston.service auto-start; agl-compositor-init owns the DRM seat
SYSTEMD_AUTO_ENABLE:${PN} = "disable"
