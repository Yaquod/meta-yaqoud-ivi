# x264 is disabled by default upstream; packagegroup-yaqoud-webrtc needs
# gstreamer1.0-plugins-ugly-x264 for the x264enc pipeline element.
PACKAGECONFIG:append = " x264"
