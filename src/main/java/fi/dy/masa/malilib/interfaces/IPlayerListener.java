package fi.dy.masa.malilib.interfaces;

import net.minecraft.server.network.ServerPlayerEntity;

public interface IPlayerListener
{
    default void onPlayerJoin(ServerPlayerEntity player) {}
    default void onPlayerLeave(ServerPlayerEntity player) {}
}