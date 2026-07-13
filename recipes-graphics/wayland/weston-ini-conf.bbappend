FILESEXTRAPATHS:prepend := "${THISDIR}/weston-ini-conf:"

SRC_URI:append = " \
    file://dp-1-0.cfg \
    file://screen-share.cfg \
    file://pipewire.cfg \
"

WESTON_DISPLAYS:yaqoud-orin-nano = "dp-1-0"
WESTON_FRAGMENTS_BASE:append:yaqoud-orin-nano = " screen-share pipewire"

# agl-compositor's module loader only resolves the "wet_module_init" symbol
# (src/compositor.c load_modules()), which plain weston's remoting-plugin.so
# does not export (it exports "weston_module_init" instead) -- loading it
# null-derefs and SIGSEGVs agl-compositor. Do NOT add remoting-plugin.so to
# modules= here. Streaming instead uses agl-compositor's native, ABI-safe
# pipewire-backend.so (a real backend, loaded via weston_compositor_load_backend(),
# not the modules= loader) -- see [pipewire] in pipewire.cfg and
# clients/agl-stream-pipewire-output in the agl-compositor source for the
# reference capture pipeline (pipewiresrc target-object=weston.pipewire).
do_compile:prepend() {
    sed -i '/^\[core\]/a backend=drm-backend.so,pipewire-backend.so' ${UNPACKDIR}/core.cfg
}
