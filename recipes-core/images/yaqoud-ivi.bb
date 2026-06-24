require recipes-graphics/images/core-image-weston.bb

DESCRIPTION = "Yaqoud IVI image with CUDA/TensorRT and Docker support for Autoware"

# CUDA / cuDNN / TensorRT (meta-tegra)
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

# IMAGE_ROOTFS_EXTRA_SPACE = "20971520"


# Docker + NVIDIA Container Runtime (meta-tegra/external/virtualization-layer)
IMAGE_INSTALL:append = " \
    nvidia-container-toolkit \
    libnvidia-container \
    nvidia-docker \
    tegra-container-passthrough \
    tegra-configs-container-csv \
    tegra-configs-udev \
    docker-moby \
    flutter-auto-app \
    flutter-ivi \
"


IMAGE_INSTALL:append = " \
    l4t-usb-device-mode \
    chrony \
"

DISTRO_FEATURES:append = " virtualization"
IMAGE_INSTALL:append = " "
