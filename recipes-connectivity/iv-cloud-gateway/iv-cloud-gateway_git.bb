SUMMARY = "Vehicle Cloud Gateway: MQTT/gRPC/zenoh bridge between the vehicle and the cloud backend"
DESCRIPTION = "A dedicated communication bridge between the vehicle's internal \
systems (main application, sensors, Autoware) and the external cloud backend. \
Handles bidirectional telemetry/command traffic over MQTT, gRPC and zenoh."
HOMEPAGE = "https://github.com/Yaquod/iv-cloud-gateway"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

# gitsm:// (not git://) -- the project vendors nlohmann/json, spdlog,
# googletest, yaml-cpp and async-mqtt5 as git submodules and builds them from
# source itself (add_subdirectory(third_party)), rather than find_package()-ing
# system copies, so they need to come down with the clone.
#
# SRCREV 29b07f4 (merge of "config: load gateway settings from environment /
# .env", parent f4ad008a which the previous pin used) replaces the old
# hardcoded main() config with gateway::Config::from_env() -- reads a .env
# from GATEWAY_ENV_FILE (default ".env" in the CWD) at process startup, real
# env vars win over the file. See files/gateway.env.sample / gateway.env.real
# below and the systemd unit's WorkingDirectory.
# git://;gitsubmodules=1 left all 5 submodules uninitialized ("git submodule
# status" showed every one prefixed "-", none checked out) -- do_configure
# then failed with "source directory ... does not contain a CMakeLists.txt
# file" for third_party/{json,spdlog,yaml-cpp,googletest}. gitsm:// is the
# fetcher that's actually worked for submodules elsewhere in this layer
# (flutter-auto's shell/plugins, cef, libchromium-nc).
SRC_URI = " \
    gitsm://github.com/Yaquod/iv-cloud-gateway.git;protocol=https;branch=main \
    file://iv-cloud-gateway.service \
    file://gateway.env.sample \
    file://gateway.env.real \
    file://grpc_cpp_plugin_target.cmake \
"
SRCREV = "29b07f428768d2d446546e03ea7528ac53f1acd5"

# protoc/grpc_cpp_plugin run on the build host during codegen even though the
# resulting binary is built for the target -- need both the -native codegen
# tools and the target runtime libraries.
DEPENDS = " \
    grpc-native grpc \
    protobuf-native protobuf \
    openssl \
    boost \
    zenoh-c \
    zenoh-cpp \
"

inherit cmake systemd useradd

# gRPCConfig.cmake (staged from the target grpc recipe) only include()s
# gRPCPluginTargets.cmake -- which defines the gRPC::grpc_cpp_plugin IMPORTED
# target -- when NOT CMAKE_CROSSCOMPILING (correct upstream gRPC behavior: a
# cross-compiled plugin binary isn't runnable as a build-time codegen tool).
# This project's proto/CMakeLists.txt unconditionally references
# $<TARGET_FILE:gRPC::grpc_cpp_plugin> regardless, so
# -DgRPC_CPP_PLUGIN_EXECUTABLE=... alone (a plain path variable) doesn't
# satisfy it -- CMAKE_PROJECT_<name>_INCLUDE injects
# files/grpc_cpp_plugin_target.cmake right after project(), defining that
# target ourselves against the native tool.
EXTRA_OECMAKE = " \
    -DBUILD_TESTS=OFF \
    -DBUILD_INTEGRATION_TESTS=OFF \
    -DENABLE_COVERAGE=OFF \
    -DENABLE_SANITIZERS=OFF \
    -DProtobuf_PROTOC_EXECUTABLE=${STAGING_BINDIR_NATIVE}/protoc \
    -DgRPC_CPP_PLUGIN_EXECUTABLE=${STAGING_BINDIR_NATIVE}/grpc_cpp_plugin \
    -DGRPC_CPP_PLUGIN_NATIVE_EXECUTABLE=${STAGING_BINDIR_NATIVE}/grpc_cpp_plugin \
    -DCMAKE_PROJECT_VehicleCloudGateway_INCLUDE=${UNPACKDIR}/grpc_cpp_plugin_target.cmake \
"

# Upstream's own CMakeLists.txt only installs a systemd unit if
# systemd/vehicle-gateway.service exists in the source tree -- it doesn't
# (checked upstream at SRCREV above), so ship and install our own instead of
# relying on that guarded install() rule.
#
# .env: gateway.env.real (gitignored, never committed -- see .gitignore) holds
# the real deployment values when present; otherwise fall back to the
# committed placeholder gateway.env.sample and warn. Same pattern as
# recipes-graphics/flutter-ivi/flutter-ivi.bb, except this one is read from
# disk at process startup (not baked into a binary), so it's a CONFFILE the
# operator can edit on-device without rebuilding.
do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/iv-cloud-gateway.service \
        ${D}${systemd_system_unitdir}/iv-cloud-gateway.service

    install -d ${D}${sysconfdir}/vehicle-gateway
    if [ -s ${UNPACKDIR}/gateway.env.real ]; then
        install -m 0600 ${UNPACKDIR}/gateway.env.real ${D}${sysconfdir}/vehicle-gateway/.env
    else
        install -m 0600 ${UNPACKDIR}/gateway.env.sample ${D}${sysconfdir}/vehicle-gateway/.env
        bbwarn "iv-cloud-gateway: using gateway.env.sample placeholders for the deployed .env -- add recipes-connectivity/iv-cloud-gateway/files/gateway.env.real with real target values"
    fi

    # vehicle_gateway dynamically links libproto_lib.so (the generated
    # protobuf/grpc code, built as its own internal shared lib by
    # proto/CMakeLists.txt) with no RPATH override, so it resolves via the
    # standard ${libdir} search path -- but upstream's own install() rules
    # never install it at all, only the vehicle_gateway executable. Without
    # this the binary can't even start on the real device ("error while
    # loading shared libraries: libproto_lib.so").
    install -d ${D}${libdir}
    install -m 0755 ${B}/proto/libproto_lib.so ${D}${libdir}/
}

# vehicle_gateway ships pre-stripped from its own build (no -s/strip flag in
# this recipe's EXTRA_OECMAKE -- upstream's own CMake config strips it),
# which conflicts with Yocto's own stripping pass. Functionally harmless,
# just means no -dbg symbols are available for this binary.
#
# buildpaths: the KNOWN UPSTREAM ISSUE documented at the bottom of this file
# (CREDENTIALS_DIR baked in from CMAKE_SOURCE_DIR) -- confirmed nothing a
# Yocto recipe can fix without an upstream source change.
INSANE_SKIP:${PN} += "already-stripped buildpaths"

# The default -dev FILES pulls in any unversioned ${libdir}/lib*.so via
# FILES_SOLIBSDEV (the convention: a real versioned .so in the main package,
# an unversioned symlink in -dev) -- libproto_lib.so is an internal
# implementation-detail library with no SONAME/version at all, so that
# default wrongly claims the one real file for -dev ("[dev-elf] -dev package
# contains non-symlink .so"), which then makes the main package RDEPENDS on
# -dev too ("[dev-deps]", since vehicle_gateway needs it at runtime). Drop
# the SOLIBSDEV glob from -dev here so FILES:${PN}'s explicit claim above wins.
FILES:${PN}-dev:remove = "${FILES_SOLIBSDEV}"

FILES:${PN} += "${systemd_system_unitdir}/iv-cloud-gateway.service ${libdir}/libproto_lib.so"
CONFFILES:${PN} += "${sysconfdir}/vehicle-gateway/.env"

SYSTEMD_SERVICE:${PN} = "iv-cloud-gateway.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

USERADD_PACKAGES = "${PN}"
USERADD_PARAM:${PN} = "--system --no-create-home --shell /sbin/nologin iv-gateway"

# .env is installed 0600 with no explicit owner, so it stayed root:root -- but
# the service runs as User=iv-gateway (above), which then can't READ it at
# all. gateway::Config::from_env() apparently treats "exists but unreadable"
# the same as "missing" and silently falls back to hardcoded defaults instead
# of erroring, which is why MQTT_BROKER=yaquod.duckdns.org in the real .env
# never took effect on-device (confirmed: the running binary was resolving
# the literal string "localhost" instead). chown to the same user this
# recipe's own USERADD_PARAM creates, once it exists.
pkg_postinst:${PN}() {
    chown iv-gateway:iv-gateway $D${sysconfdir}/vehicle-gateway/.env
}

# zenoh-c's cargo-built libzenohc.so isn't always picked up correctly by the
# shlibs scanner; depend on it explicitly at runtime too, on top of DEPENDS.
RDEPENDS:${PN} += "zenoh-c"

# KNOWN UPSTREAM ISSUE (not a packaging problem, nothing to fix here): auth
# credential loading in src/services/auth_services.cc is compiled against
# CREDENTIALS_DIR="${CMAKE_SOURCE_DIR}/credentials" -- an absolute build-host
# source path baked in at compile time, and that directory doesn't even exist
# in the upstream repo. This will not resolve on the installed target and is
# not something a Yocto recipe can fix; needs an upstream code change (e.g. an
# installed/configurable credentials path) before auth actually works on-device.
