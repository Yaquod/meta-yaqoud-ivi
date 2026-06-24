SUMMARY = "Pre-pull Autoware agent Docker image into rootfs at build time"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://autoware-preload.service"
S = "${WORKDIR}"

inherit systemd allarch

AUTOWARE_IMAGE = "ghcr.io/yaquod/autoware-agent-runtime:latest"
AUTOWARE_TAR = "autoware-agent-runtime.tar"

do_fetch[network] = "1"
do_compile[nostamp] = "1"

do_compile() {
    if ! docker image inspect "${AUTOWARE_IMAGE}" > /dev/null 2>&1; then
        docker pull "${AUTOWARE_IMAGE}"
    fi
    docker save "${AUTOWARE_IMAGE}" -o "${WORKDIR}/${AUTOWARE_TAR}"
}

do_install() {
    install -d ${D}/opt/docker-preload
    install -m 0644 ${WORKDIR}/${AUTOWARE_TAR} \
        ${D}/opt/docker-preload/${AUTOWARE_TAR}

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/autoware-preload.service \
        ${D}${systemd_unitdir}/system/autoware-preload.service
}

FILES:${PN} = "/opt/docker-preload/${AUTOWARE_TAR} \
               ${systemd_unitdir}/system/autoware-preload.service"

SYSTEMD_SERVICE:${PN} = "autoware-preload.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"