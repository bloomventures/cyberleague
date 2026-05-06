const std = @import("std");

pub fn build(b: *std.Build) void {
    const optimize = b.standardOptimizeOption(.{});
    const target = b.standardTargetOptions(.{});
    const exe = b.addExecutable(.{
        .name = "bot",
        .root_module = b.createModule(.{
            .optimize = optimize,
            .target = target,
            .root_source_file = b.path("src/main.zig"),
        }),
    });
    b.installArtifact(exe);

    const run_step = b.step("run", "run the program");
    const run_exe = b.addRunArtifact(exe);
    run_step.dependOn(&run_exe.step);

    const check_step = b.step("check", "check step");
    const check_exe = b.addExecutable(.{
        .name = "check",
        .root_module = exe.root_module,
    });
    check_step.dependOn(&check_exe.step);
}
