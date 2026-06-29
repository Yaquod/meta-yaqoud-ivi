require recipes-core/images/core-image-base.bb

DESCRIPTION = "Yaqoud IVI image with AGL compositor, CUDA/TensorRT and Docker support"

IMAGE_FEATURES += "splash package-management ssh-server-dropbear hwcodecs"

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
IMAGE_INSTALL:append = " \
    nvidia-container-toolkit \
    libnvidia-container \
    nvidia-docker \
    tegra-container-passthrough \
    tegra-configs-container-csv \
    tegra-configs-udev \
    docker-moby \
    docker \
    autoware-agent-service \
"

# Flutter
IMAGE_INSTALL:append = " \
    flutter-auto \
    flutter-auto-app \
    flutter-ivi \
    flutter-cluster \
"

# System
IMAGE_INSTALL:append = " \
    l4t-usb-device-mode \
    chrony \
    networkmanager \
    networkmanager-nmcli \
    iproute2 \
    net-tools \
    agl-driver-linger \
"

DISTRO_FEATURES:append = " virtualization wayland"

# weston-init conflicts (RCONFLICTS) and file-collides (/etc/xdg/weston/weston.ini)
# with agl-compositor-init's weston-ini dependency. agl-compositor-init owns the
# DRM seat; weston-init must never land in this image, however it gets pulled in
# (explicit IMAGE_INSTALL, packagegroup-base-extended, or a recommendation chain).
BAD_RECOMMENDATIONS += "weston-init"
IMAGE_INSTALL:remove = "weston-init"