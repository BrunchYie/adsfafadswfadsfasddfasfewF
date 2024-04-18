package fi.dy.masa.malilib.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;

/**
 * This can be used as a replacement for your "PacketByteBuf" type of CustomPayloads,
 * And can be used to try to (re)-implement some of your old IPluginChannelHandler based
 * protocols, if needed; but generally using an NbtCompound Payload is simple enough.
 */
public class MaLiLibByteBuf extends PacketByteBuf
{
    public MaLiLibByteBuf(ByteBuf parent)
    {
        super(parent);
    }
}