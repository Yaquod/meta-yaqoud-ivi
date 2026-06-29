FILESEXTRAPATHS:prepend := "${THISDIR}/weston-ini-conf:"

SRC_URI:append = " \
    file://dp-1-0.cfg \
    file://screen-share.cfg \
"

WESTON_DISPLAYS:yaqoud-orin-nano = "dp-1-0"
WESTON_FRAGMENTS_BASE:append:yaqoud-orin-nano = " screen-share"

do_compile:prepend() {
    sed -i '/^\[core\]/a backend=drm-backend.so' ${UNPACKDIR}/core.cfg
}
