FILESEXTRAPATHS:prepend := "${THISDIR}/agl-compositor-init:"

AGL_KVM_REMOTE_OUTPUT_IP = "3.80.34.181"
AGL_KVM_REMOTE_OUTPUT_PORT = "5005"

# agl-compositor and agl-stream-pipewire-output run as User=agl-driver, but the
# system PipeWire socket (/run/pipewire/pipewire-0) is group "pipewire" mode 0660,
# so agl-driver can't open it without being granted that group -- the compositor's
# pipewire-backend.so then never registers the "weston.pipewire" node and the
# streamer fails with "stream error: target not found". Neither unit orders itself
# after pipewire/wireplumber either, so even with permissions fixed there's a boot
# race. Add both as drop-ins.
SRC_URI += " \
    file://03-agl-compositor-pipewire-perms.conf \
    file://agl-compositor-stream-pipewire-perms.conf \
"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}/agl-compositor.service.d
    install -m644 ${UNPACKDIR}/03-agl-compositor-pipewire-perms.conf \
        ${D}${systemd_system_unitdir}/agl-compositor.service.d/03-agl-compositor-pipewire-perms.conf

    install -d ${D}${systemd_system_unitdir}/agl-compositor-stream-pipewire.service.d
    install -m644 ${UNPACKDIR}/agl-compositor-stream-pipewire-perms.conf \
        ${D}${systemd_system_unitdir}/agl-compositor-stream-pipewire.service.d/01-perms.conf
}

FILES:${PN} += "\
    ${systemd_system_unitdir}/agl-compositor.service.d/03-agl-compositor-pipewire-perms.conf \
    ${systemd_system_unitdir}/agl-compositor-stream-pipewire.service.d \
    ${systemd_system_unitdir}/agl-compositor-stream-pipewire.service.d/01-perms.conf \
"
