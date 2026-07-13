# nvidia-docker's own /etc/docker/daemon.json (just the nvidia default-runtime
# stanza) conflicts at rootfs assembly with autoware-agent-stack's
# /etc/docker/daemon.json (recipes-autoware/autoware-agent-stack/files/daemon.json
# in meta-autoware-edge) -- a strict superset: the same nvidia runtime config,
# plus the DNS override Autoware's image pulls/builds need (Jetson's
# systemd-resolved stub isn't reachable from inside a container) and log
# rotation. Both recipes are only combined in this image since
# autoware-agent-stack was merged into yaqoud-ivi.bb's IMAGE_INSTALL -- drop
# nvidia-docker's copy so autoware-agent-stack's is the sole conffile.
do_install:append() {
    rm -f ${D}${sysconfdir}/docker/daemon.json
}
