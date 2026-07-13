SUMMARY = "GStreamer RTSP-push stack for IVI screen streaming (Orin Nano -> MediaMTX)"
LICENSE = "MIT"

inherit packagegroup

# x264enc lives in gstreamer1.0-plugins-ugly (GPL) — must accept the flag.
# Also add to your image/distro conf: LICENSE_FLAGS_ACCEPTED += "commercial".
LICENSE_FLAGS_ACCEPTED += "commercial"


RDEPENDS:${PN} = " \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-base-app \
    gstreamer1.0-plugins-base-videoconvertscale \
    gstreamer1.0-plugins-base-videorate \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly \
    gstreamer1.0-plugins-ugly-x264 \
    gstreamer1.0-rtsp-server \
    gstreamer1.0-pipewire \
    pipewire \
"
RDEPENDS:${PN} += "\
    weston-pipewire \
    gstreamer1.0-plugins-good-rtpmanager \
    gstreamer1.0-plugins-good-rtp \
    gstreamer1.0-plugins-good-udp \
    gstreamer1.0-plugins-good-jpeg \
    agl-compositor-init-pipewire \
"