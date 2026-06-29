SUMMARY = "Zenoh C API"
DESCRIPTION = "C binding for the Eclipse Zenoh protocol"
HOMEPAGE = "https://github.com/eclipse-zenoh/zenoh-c"
LICENSE = "Apache-2.0 & EPL-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=530d837aca648e45704db71dedff39c4"

inherit cargo

# Add chrpath-native to strip bad rpaths during install
DEPENDS += "chrpath-native"

SRC_URI = "git://github.com/eclipse-zenoh/zenoh-c.git;protocol=https;branch=main"
SRCREV = "b4628cd6fd65a2f2067f163466c3cb248f783b55"

do_compile[network] = "1"
CARGO_DISABLE_BITBAKE_VENDORING = "1"
CARGO_SRC_DIR = ""
CARGO_TARGET_DIR = "${S}/target"
EXTRA_OECARGO = "--release"

do_compile() {
    cd ${S}
    cargo build --target-dir ${CARGO_TARGET_DIR} ${EXTRA_OECARGO}
}

do_install() {
    install -d ${D}${libdir}
    ZENOHC_LIB=$(find ${S}/target -name "libzenohc.so" -path "*/release/*" | head -1)
    if [ -z "$ZENOHC_LIB" ]; then
        bbfatal "libzenohc.so not found in ${S}/target/"
    fi
    
    # Strip the bad RPATH injected by Cargo
    chrpath -d "$ZENOHC_LIB"
    
    install -m 755 "$ZENOHC_LIB" ${D}${libdir}/

    install -d ${D}${includedir}
    for h in ${S}/include/zenoh*.h; do
        install -m 644 "$h" ${D}${includedir}/
    done

    install -d ${D}${libdir}/cmake/zenohc
    cat > ${D}${libdir}/cmake/zenohc/zenohc-config.cmake <<EOF
include(CMakeFindDependencyMacro)

get_filename_component(ZENOHC_DIR "\${CMAKE_CURRENT_LIST_DIR}/../../../" ABSOLUTE)

set(zenohc_FOUND TRUE)
set(zenohc_INCLUDE_DIR "\${ZENOHC_DIR}/include")
set(zenohc_LIBRARY "\${ZENOHC_DIR}/lib/libzenohc.so")

if(NOT TARGET zenohc::lib)
  add_library(zenohc::lib SHARED IMPORTED)
  set_target_properties(zenohc::lib PROPERTIES
    IMPORTED_LOCATION "\${zenohc_LIBRARY}"
    INTERFACE_INCLUDE_DIRECTORIES "\${zenohc_INCLUDE_DIR}"
  )
endif()
EOF

    cat > ${D}${libdir}/cmake/zenohc/zenohc-config-version.cmake <<EOF
set(PACKAGE_VERSION 1.9.0)
if("\${PACKAGE_FIND_VERSION}" VERSION_EQUAL "1.9.0" OR
   "\${PACKAGE_FIND_VERSION}" VERSION_LESS "1.9.0")
  set(PACKAGE_VERSION_COMPATIBLE TRUE)
else()
  set(PACKAGE_VERSION_COMPATIBLE FALSE)
endif()
if("\${PACKAGE_FIND_VERSION}" VERSION_EQUAL "1.9.0")
  set(PACKAGE_VERSION_EXACT TRUE)
endif()
EOF
}

FILES:${PN} = "${libdir}/libzenohc.so"
FILES:${PN}-dev = "${includedir}/* ${libdir}/cmake/*"

# Skip buildpaths (due to Rust debug info) and ldflags
INSANE_SKIP:${PN} = "ldflags buildpaths"
INSANE_SKIP:${PN}-dev = "ldflags buildpaths"
