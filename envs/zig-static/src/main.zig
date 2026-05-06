const std = @import("std");

const Input = struct {
    ping: ?std.json.Value = null,
    // Using a quoted identifier to access a field that isn't valid zig
    //@"player-cards": ?[]u32 = null,
};

pub fn main(init: std.process.Init) !void {
    const arena = init.arena.allocator();
    const io = init.io;

    const stdout = std.Io.File.stdout();
    var out_buf: [0x1000]u8 = undefined;
    var writer = stdout.writer(io, &out_buf);

    const stdin = std.Io.File.stdin();
    var in_buf: [0x1000]u8 = undefined;
    var reader = stdin.reader(io, &in_buf);

    const input = try reader.interface.allocRemaining(arena, .unlimited);
    const json = try std.json.parseFromSliceLeaky(Input, arena, input, .{
        .ignore_unknown_fields = true,
    });

    if (json.ping) |ping| {
        try writer.interface.print("{{\"pong\":{f}}}", .{ std.json.fmt(ping, .{}) });
    }
    try writer.flush();
}
