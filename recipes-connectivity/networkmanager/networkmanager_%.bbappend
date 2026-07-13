FILESEXTRAPATHS:prepend := "${THISDIR}/networkmanager:"

# wlan0 comes up fine at the driver/NetworkManager level (rfkill unblocked,
# wpa_supplicant initializes) but there is no saved connection profile for any
# SSID anywhere in the image, so NetworkManager just idles and rescans forever
# without ever associating. Ship a default profile so it auto-connects on first
# boot with zero manual `nmcli` steps.
#
# EDIT recipes-connectivity/networkmanager/networkmanager/lab-wifi.nmconnection
# with the real SSID/password before building -- CHANGEME_* placeholders below
# will not connect to anything as-is.
SRC_URI += "file://lab-wifi.nmconnection"

do_install:append() {
    install -d ${D}${sysconfdir}/NetworkManager/system-connections
    install -m 0600 ${UNPACKDIR}/lab-wifi.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/lab-wifi.nmconnection
}

FILES:${PN}-daemon += "${sysconfdir}/NetworkManager/system-connections/lab-wifi.nmconnection"
