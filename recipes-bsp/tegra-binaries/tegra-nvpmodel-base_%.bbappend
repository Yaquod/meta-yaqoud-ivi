# Default nvpmodel.conf (from meta-tegra) boots into "25W" (PM_CONFIG DEFAULT=1),
# not the board's maximum-performance mode. Confirmed on-device this throttling
# is enough on its own to make Autoware Universe's trajectory planner miss its
# 1s real-time deadline once actual behavior-planning/control load kicks in
# during a real drive (not just idle/query), which the safety monitor correctly
# treats as a fault and triggers a real MRM emergency-stop -- the vehicle
# engages, drives briefly, then stops mid-route every time. Switching to
# MAXN_SUPER (mode 2) plus jetson_clocks (see jetson-clocks-boot.bb) resolved it
# outright: same trip, same coordinates, clean arrival at pickup, zero MRM
# events. This is a board-wide power setting, not specific to any one workload.
NVPMODEL_CONFIG_DEFAULT = "2"
