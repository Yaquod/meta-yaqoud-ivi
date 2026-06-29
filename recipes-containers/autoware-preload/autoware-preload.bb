# SUMMARY = "Systemd unit to pull the Autoware agent Docker image from GHCR at boot"
# DESCRIPTION = "Installs a oneshot systemd service that runs 'docker pull' against \
# ghcr.io/yaquod/autoware-agent-runtime at boot, before the actual agent service \
# starts. This replaces the earlier tarball-preload approach (which hit GitHub \
# Release's 2GB asset size limit) -- the image is fetched live from GHCR instead \
# of being baked into the rootfs. Requires network connectivity at boot."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# SRC_URI = "file://autoware-pull.service"

# S = "${WORKDIR}"

# inherit systemd allarch
# # allarch is correct here again -- unlike the tarball version, this package
# # now only contains a systemd unit file (plain text), no architecture-specific
# # binary payload. No COMPATIBLE_MACHINE restriction needed.

# do_install() {
#     install -d ${D}${systemd_unitdir}/system
#     install -m 0644 ${WORKDIR}/autoware-pull.service \
#         ${D}${systemd_unitdir}/system/autoware-pull.service
# }

# FILES:${PN} = "${systemd_unitdir}/system/autoware-pull.service"

# SYSTEMD_SERVICE:${PN} = "autoware-pull.service"
# SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# # This package only makes sense alongside Docker itself being present in the
# # image; it does not pull in docker-moby/nvidia-container-toolkit etc. itself
# # since those belong at the image/distro level (as in your
# # yaqoud-orin-nano-image.bb), not as a dependency of this single-purpose
# # package. Uncomment if you want BitBake to enforce it explicitly:
# # RDEPENDS:${PN} = "docker-moby"
