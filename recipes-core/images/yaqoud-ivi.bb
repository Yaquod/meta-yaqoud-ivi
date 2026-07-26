require recipes-core/images/core-image-base.bb

DESCRIPTION = "Yaqoud IVI image with AGL compositor, CUDA/TensorRT and Docker support"

IMAGE_FEATURES += "splash package-management ssh-server-dropbear hwcodecs"

# core-image-base (required above) never sets a default systemd target, so it
# falls back to multi-user.target -- but agl-compositor.service (meta-agl-core,
# not ours: Before=graphical.target, WantedBy=graphical.target) and
# flutter-auto-ivi-app.service (WantedBy=graphical.target) are both designed
# assuming the system actually boots INTO graphical.target. Without this,
# neither ever auto-starts on boot (confirmed on-device: both sit
# "inactive (dead)" with zero journal entries -- never even attempted, not
# crashed -- until manually `systemctl start`ed). Set it the same way
# `systemctl set-default graphical.target` does, at image build time.
IMAGE_PREPROCESS_COMMAND += "set_default_systemd_target; "
set_default_systemd_target () {
    ln -sf ${systemd_unitdir}/system/graphical.target \
        ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/default.target
}

# Graphics / Wayland / AGL compositor
# NOTE: weston-init is intentionally excluded (see BAD_RECOMMENDATIONS/IMAGE_INSTALL:remove below) -
# it conflicts with agl-compositor-init (RCONFLICTS) and both ship /etc/xdg/weston/weston.ini.
# agl-compositor-init pulls in weston-ini for that config file instead.
IMAGE_INSTALL:append = " \
    weston \
    wayland \
    libgles2 \
    libegl \
    mesa \
    mesa-megadriver \
    xkeyboard-config \
    agl-compositor \
    agl-compositor-init \
"

# CUDA / cuDNN / TensorRT
IMAGE_INSTALL:append = " \
    cuda-toolkit \
    cuda-driver \
    cuda-compiler \
    cuda-libraries \
    cuda-cudart \
    cuda-nvcc \
    cuda-command-line-tools \
    cudnn \
    tensorrt-core \
    tensorrt-trtexec \
    tegra-cuda-utils \
    tegra-libraries-cuda \
"

# Docker + NVIDIA Container Runtime
# NOTE: the old ad-hoc autoware-agent-service/autoware-preload units that used
# to live in this layer are gone. meta-autoware-edge's autoware-edge-image is
# still buildable standalone, but its actual stack (autoware-agent-stack,
# awsim-shinjuku-map, rootfs-expand) is now ALSO merged in below so it runs
# on this same image -- no separate flash needed.
IMAGE_INSTALL:append = " \
    nvidia-container-toolkit \
    libnvidia-container \
    nvidia-docker \
    tegra-container-passthrough \
    tegra-configs-container-csv \
    tegra-configs-udev \
    docker-moby \
    docker \
    docker-compose \
"

# Autoware edge stack (merged from meta-autoware-edge/recipes-core/images/
# autoware-edge-image.bb -- see that file for the source list this mirrors).
# autoware-agent-stack: docker-compose project + systemd unit that pulls and
# runs the Autoware + agent images (/opt/autoware-edge). awsim-shinjuku-map:
# map data bind-mounted into it. rootfs-expand: first-boot partition/fs grow
# so /var/lib/docker has room for the (large) Autoware images -- needs
# parted (partition resize; growpart isn't packaged in any layer in this
# build), e2fsprogs-resize2fs, util-linux (lsblk/findmnt). jetson-clocks-boot:
# locks clocks to nvpmodel's MAXN_SUPER maximum at every boot (see
# recipes-bsp/tegra-binaries/jetson-clocks-boot.bb and the
# tegra-nvpmodel-base_%.bbappend next to it) -- confirmed on-device that
# without this, Autoware Universe's trajectory planner intermittently misses
# its 1s real-time deadline during an actual drive and the vehicle hits a
# real MRM emergency-stop mid-route.
IMAGE_INSTALL:append = " \
    curl \
    git \
    e2fsprogs-resize2fs \
    parted \
    util-linux \
    tegra-tools \
    rootfs-expand \
    autoware-agent-stack \
    awsim-shinjuku-map \
    jetson-clocks-boot \
"

# autoware-edge-image's own user; docker group membership so the compose
# stack's containers/CLI work without root.
EXTRA_USERS_PARAMS:append = " \
    useradd -p '' autoware; \
    usermod -aG docker autoware; \
"

# Flutter
# libchromium-nc/spotifyd/avahi-daemon/ca-certificates are already pulled in as
# RDEPENDS of flutter-ivi (see recipes-graphics/flutter-ivi/flutter-ivi.bb) --
# listed explicitly here too so the full app stack is visible at a glance.
IMAGE_INSTALL:append = " \
    flutter-auto \
    flutter-auto-app \
    flutter-ivi \
    flutter-cluster \
    libchromium-nc \
    spotifyd \
    avahi-daemon \
    ca-certificates \
"

# Vehicle Cloud Gateway (MQTT/gRPC/zenoh bridge to the fleet backend). Nothing
# else RDEPENDS on this, so it needs to be listed explicitly to land in the image.
IMAGE_INSTALL:append = " iv-cloud-gateway"

# System
IMAGE_INSTALL:append = " \
    l4t-usb-device-mode \
    chrony \
    fake-hwclock \
    networkmanager \
    networkmanager-nmcli \
    iproute2 \
    net-tools \
    agl-driver-linger \
"

# The PMIC RTC (nvvrs-pseq-rtc) on this devkit has no persisted/valid time and
# resets to 1970-01-01 on every cold boot. fake-hwclock saves the clock on
# shutdown and restores it as a best-effort estimate at early boot, before
# network/NTP (chrony) is available to correct it for real.

# x11 is client-libraries-only here (libxcb/libxrandr) -- no X server, no
# XWayland, agl-compositor stays the sole compositor. It's required because
# CEF's official prebuilt libcef.so (recipes-graphics/chromium-dart/cef_128.bb)
# has BOTH its X11 and Wayland Ozone backends compiled into the one binary
# (confirmed directly in the linked symbols), so libxcb.so.1/libXrandr.so.2
# are hard ELF NEEDED entries the dynamic linker requires at process start
# regardless of always launching with --ozone-platform=wayland.
DISTRO_FEATURES:append = " virtualization wayland x11"

# weston-init conflicts (RCONFLICTS) and file-collides (/etc/xdg/weston/weston.ini)
# with agl-compositor-init's weston-ini dependency. agl-compositor-init owns the
# DRM seat; weston-init must never land in this image, however it gets pulled in
# (explicit IMAGE_INSTALL, packagegroup-base-extended, or a recommendation chain).
BAD_RECOMMENDATIONS += "weston-init"
IMAGE_INSTALL:remove = "weston-init"

IMAGE_INSTALL:append = " packagegroup-yaqoud-webrtc"
LICENSE_FLAGS_ACCEPTED:append = " commercial"