SUMMARY = "A Flutter IVI application"
DESCRIPTION = "An Example Flutter application for the ivi"
AUTHOR = "Ahmed Wafdy"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

SRCREV = "7e0bb4faa4d5f92fc0cf0d64ffd1869347c575a9"
SRC_URI = "git://github.com/Yaquod/flutter-ivi.git;lfs=0;branch=main;protocol=https"

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "flutter_ivi"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "yaqoud-flutter-ivi"
PUBSPEC_IGNORE_LOCKFILE = "1"

inherit flutter-app