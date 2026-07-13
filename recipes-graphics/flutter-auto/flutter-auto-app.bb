SUMMARY = "Flutter Auto IVI app systemd service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " file://flutter-auto-ivi-app.service \
    file://flutter-auto-cluster-app.service \
"

S = "${UNPACKDIR}"

inherit systemd

# agl_shell (the AGL Wayland shell protocol) is a compositor-enforced
# singleton -- only ONE client per compositor can bind it, and both this
# service and flutter-auto-ivi-app.service request --window-type=BG, which
# binds it. When both auto-start, whichever loses the race gets a FATAL
# "[Display] shell 'agl' handshake failed" and exit(1)s, then crash-loops
# forever (Restart=on-failure) since it never wins the race against the
# other's already-successful bind. No physical second display exists yet
# (future plan: WebRTC-share both to web, not a second panel) -- split the
# cluster service into its own package, installed but NOT auto-enabled, so
# it doesn't compete with the ivi-app for agl_shell. `systemctl enable
# --now flutter-auto-cluster-app` once there's an actual second output to
# target and the two are reconciled (e.g. one process managing both, or the
# second one using a non-BG window-type).
PACKAGES =+ "${PN}-cluster"
SYSTEMD_SERVICE:${PN}-cluster = "flutter-auto-cluster-app.service"
SYSTEMD_AUTO_ENABLE:${PN}-cluster = "disable"
FILES:${PN}-cluster = "${systemd_system_unitdir}/flutter-auto-cluster-app.service"

SYSTEMD_SERVICE:${PN} += "flutter-auto-ivi-app.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# Still install the (disabled) cluster unit alongside the ivi-app by default,
# ready for a manual `systemctl enable --now` later -- just don't auto-start it.
RRECOMMENDS:${PN} += "${PN}-cluster"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/flutter-auto-ivi-app.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/flutter-auto-cluster-app.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} += "${systemd_system_unitdir}/flutter-auto-ivi-app.service"
