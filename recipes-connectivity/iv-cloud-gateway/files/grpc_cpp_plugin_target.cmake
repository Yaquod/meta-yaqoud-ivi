# gRPCConfig.cmake only include()s gRPCPluginTargets.cmake (which defines the
# gRPC::grpc_cpp_plugin IMPORTED target) when NOT CMAKE_CROSSCOMPILING -- correct
# upstream gRPC behavior, since a cross-compiled plugin binary wouldn't be
# runnable as a build-time codegen tool anyway. This project's proto/CMakeLists.txt
# unconditionally references $<TARGET_FILE:gRPC::grpc_cpp_plugin> though, so define
# it ourselves here, pointing at the NATIVE grpc_cpp_plugin (it runs on the build
# host during codegen even though the generated code is compiled for the target).
#
# Injected via CMAKE_PROJECT_<name>_INCLUDE, which CMake include()s right after
# the top-level project() call -- before proto/ (a subdirectory) is processed.
if (NOT TARGET gRPC::grpc_cpp_plugin)
    add_executable(gRPC::grpc_cpp_plugin IMPORTED)
    set_target_properties(gRPC::grpc_cpp_plugin PROPERTIES
        IMPORTED_LOCATION "${GRPC_CPP_PLUGIN_NATIVE_EXECUTABLE}"
    )
endif ()
