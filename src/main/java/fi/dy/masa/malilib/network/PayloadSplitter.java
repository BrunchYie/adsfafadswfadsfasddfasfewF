package fi.dy.masa.malilib.network;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.ApiStatus;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

/**
 * Network packet splitter code from QuickCarpet by skyrising
 * @author skyrising
 *
 */
@ApiStatus.Experimental
public class PayloadSplitter
{
    public static final int MAX_TOTAL_PER_PACKET_S2C = 1048576;
    public static final int MAX_PAYLOAD_PER_PACKET_S2C = MAX_TOTAL_PER_PACKET_S2C - 5;
    public static final int MAX_TOTAL_PER_PACKET_C2S = 32767;
    public static final int MAX_PAYLOAD_PER_PACKET_C2S = MAX_TOTAL_PER_PACKET_C2S - 5;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_C2S = 1048576;
    public static final int DEFAULT_MAX_RECEIVE_SIZE_S2C = 67108864;

    private static final Map<Pair<PacketListener, Identifier>, ReadingSession> READING_SESSIONS = new HashMap<>();

    @ApiStatus.Experimental
    public static <T extends CustomPayload> boolean send(IPluginClientPlayHandler<T> handler, Identifier channel, PacketByteBuf packet)
    {
        //ClientPlayNetworking.send(channel, packet);
        //send(packet, MAX_PAYLOAD_PER_PACKET_C2S, buf -> networkHandler.sendPacket(new CustomPayloadC2SPacket(new PacketByteBufPayload(channel, buf))));
        return send(handler, packet, MAX_PAYLOAD_PER_PACKET_C2S);
    }

    @ApiStatus.Experimental
    private static <T extends CustomPayload> boolean send(IPluginClientPlayHandler<T> handler, PacketByteBuf packet, int payloadLimit)
    {
        int len = packet.writerIndex();

        packet.resetReaderIndex();

        for (int offset = 0; offset < len; offset += payloadLimit)
        {
            int thisLen = Math.min(len - offset, payloadLimit);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer(thisLen));

            buf.resetWriterIndex();

            if (offset == 0)
            {
                buf.writeVarInt(len);
            }

            buf.writeBytes(packet, thisLen);

            //ClientPlayNetworking.send(channel, buf);
            handler.encodeWithSplitter(buf);
        }

        packet.release();

        return true;
    }

    @ApiStatus.Experimental
    public static <T extends CustomPayload> PacketByteBuf receive(IPluginClientPlayHandler<T> handler,
                                                                  PacketByteBuf buf,
                                                                  ClientPlayNetworkHandler networkHandler)
    {
        return receive(handler.getPayloadChannel(), buf, DEFAULT_MAX_RECEIVE_SIZE_S2C, networkHandler);
    }

    @Nullable
    @ApiStatus.Experimental
    private static PacketByteBuf receive(Identifier channel,
                                         PacketByteBuf buf,
                                         int maxLength,
                                         ClientPlayPacketListener networkHandler)
    {
        Pair<PacketListener, Identifier> key = new Pair<>(networkHandler, channel);

        return READING_SESSIONS.computeIfAbsent(key, ReadingSession::new).receive(readPayload(buf), maxLength);
    }

    @ApiStatus.Experimental
    public static PacketByteBuf readPayload(PacketByteBuf byteBuf)
    {
        PacketByteBuf newBuf = new PacketByteBuf(Unpooled.buffer());
        newBuf.writeBytes(byteBuf.copy());
        byteBuf.skipBytes(byteBuf.readableBytes());
        return newBuf;
    }

    @ApiStatus.Experimental
    private static class ReadingSession
    {
        private final Pair<PacketListener, Identifier> key;
        private int expectedSize = -1;
        private PacketByteBuf received;

        @ApiStatus.Experimental
        private ReadingSession(Pair<PacketListener, Identifier> key)
        {
            this.key = key;
        }

        @Nullable
        @ApiStatus.Experimental
        private PacketByteBuf receive(PacketByteBuf data, int maxLength)
        {
            if (this.expectedSize < 0)
            {
                this.expectedSize = data.readVarInt();

                if (this.expectedSize > maxLength)
                {
                    throw new IllegalArgumentException("Payload too large");
                }

                this.received = new PacketByteBuf(Unpooled.buffer(this.expectedSize));
            }

            this.received.writeBytes(data.readBytes(data.readableBytes()));

            if (this.received.writerIndex() >= this.expectedSize)
            {
                READING_SESSIONS.remove(this.key);
                return this.received;
            }

            return null;
        }
    }
}
