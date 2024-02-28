package fi.dy.masa.malilib.network.payload;

import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.network.handler.config.ClientConfigHandler;
import fi.dy.masa.malilib.network.handler.config.ServerConfigHandler;
import fi.dy.masa.malilib.network.handler.play.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.play.ServerPlayHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This is made to "manage" the payload types and do the actual channel registrations via the Fabric Network API (4.0.0+)
 * From here, we Map the payload CODEC and TYPE into a HashMap; for our own reference by the Payloads based on their PacketType_example.
 * This was done in an attempt to make the remaining functions more abstract.
 */
public class PayloadTypeRegister
{
    public static final PayloadTypeRegister INSTANCE = new PayloadTypeRegister();
    public static PayloadTypeRegister getInstance() { return INSTANCE; }
    private final Map<PayloadType, PayloadCodec> TYPES = new HashMap<>();

    public PayloadTypeRegister()
    {
        initPayloads();
    }
    public void register(PayloadType type, String key, String namespace, String path)
    {
        if (!TYPES.containsKey(type))
        {
            PayloadCodec codec = new PayloadCodec(type, key, namespace, path);
            TYPES.put(type, codec);
            MaLiLib.printDebug("PayloadTypeRegister#register(): registering a new PayloadCodec id: {} // {}:{}", codec.getId().hashCode(), codec.getId().getNamespace(), codec.getId().getPath());

        }
        else
        {
            MaLiLib.printDebug("PayloadTypeRegister#register(): type {} already exists.", type.toString());
        }
    }

    public <T extends CustomPayload> void registerPlayChannel(PayloadType type, CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> packetCodec)
    {
        PayloadCodec codec = getPayloadCodec(type);

        // Never Attempt to "re-register" a channel.  Bad things will happen.
        if (codec == null || codec.isPlayRegistered())
            return;

        codec.registerPlayCodec();

        if (PayloadTypeRegistryImpl.PLAY_S2C.get(id) != null || PayloadTypeRegistryImpl.PLAY_C2S.get(id) != null)
        {
            // This just saved Minecraft from crashing, your welcome.
            MaLiLib.logger.warn("registerPlayChannel(): blocked duplicate Play Channel registration attempt for: {}.", id.id().toString());
        }
        else
        {
            MaLiLib.printDebug("PayloadTypeRegister#registerPlayChannel(): registering Play C2S Channel: {}", id.id().toString());
            PayloadTypeRegistry.playC2S().register(id, packetCodec);
            PayloadTypeRegistry.playS2C().register(id, packetCodec);
            // We need to register the channel bi-directionally for it to work.
        }
    }

    public <T extends CustomPayload> void registerConfigChannel(PayloadType type, CustomPayload.Id<T> id, PacketCodec<PacketByteBuf, T> packetCodec)
    {
        PayloadCodec codec = getPayloadCodec(type);

        // Never Attempt to "re-register" a channel.  Bad things will happen.
        if (codec == null || codec.isConfigRegistered())
            return;
        codec.registerConfigCodec();

        if (PayloadTypeRegistryImpl.CONFIGURATION_S2C.get(id) != null || PayloadTypeRegistryImpl.CONFIGURATION_C2S.get(id) != null)
        {
            // This just saved Minecraft from crashing, your welcome.
            MaLiLib.logger.warn("registerConfigChannel(): blocked duplicate Configuration Channel registration attempt for: {}.", id.id().toString());
        }
        else
        {
            MaLiLib.printDebug("PayloadTypeRegister#registerConfigChannel(): registering Configuration C2S Channel: {}", id.id().toString());
            PayloadTypeRegistry.configurationC2S().register(id, packetCodec);
            PayloadTypeRegistry.configurationS2C().register(id, packetCodec);
            // We need to register the channel bi-directionally for it to work.
        }
    }

    /**
     * Abstract method for CustomPayload's to define their PACKET_CODEC value.
     */
    @Nullable
    public PayloadCodec getPayloadCodec(PayloadType type)
    {
        //MaLiLib.printDebug("PayloadTypeRegister#getPayloadCodec(): type: {}", type.toString());
        return TYPES.getOrDefault(type, null);
    }
    /**
     * Abstract method for CustomPayload's to define their PACKET_TYPE value, derived from the channel Identifier
     */
    @Nullable
    public Identifier getIdentifier(PayloadType type)
    {
        //MaLiLib.printDebug("PayloadTypeRegister#getIdentifier(): type: {}", type.toString());
        return TYPES.getOrDefault(type, null).getId();
    }

    /**
     * The Payload "KEY" field is simply for declaring any special "default" key Values for data if none are known,
     * Such as for example nbt.getString(KEY) -- These are not required, but can prove to be very useful.
     */
    @Nullable
    public String getKey(PayloadType type)
    {
        //MaLiLib.printDebug("PayloadTypeRegister#getKey(): type: {}", type.toString());
        return TYPES.getOrDefault(type, null).getKey();
    }

    /**
     * Search for a registered Payload type by Identifier
     */
    @Nullable
    public PayloadType getPayloadType(Identifier id)
    {
        //MaLiLib.printDebug("PayloadTypeRegister#getPayloadType(): checking for payload type: {}", id.toString());
        for (PayloadType type : TYPES.keySet())
        {
            PayloadCodec codec = TYPES.get(type);
            if (codec != null)
            {
                //MaLiLib.printDebug("PayloadTypeRegister#getPayloadType(): codec type: {} // id: {}", codec.getType(), codec.getId().toString());
                if (codec.getId().equals(id))
                {
                    return type;
                }
            }
        }
        // Not found
        return null;
    }

    /**
     * The init for this method.  This must be called at the first possible moment, so it can behave like it's static
     */
    public void initPayloads()
    {
        MaLiLib.printDebug("PayloadTypeRegister#initPayloads(): invoked.");

        // Register the play/config channel codec for every existing PayLoad in our TYPES HashMap<>,
        // or fail to register the channel, and don't attempt to handle the packets.
        register(PayloadType.CARPET_HELLO,      "carpet_hello",             "carpet",   "hello");
        //register(PayloadType.MALILIB_BYTEBUF,   "malilib_bytebuf",          "malilib",  "bytebuf");
        //register(PayloadType.SERVUX_BLOCKS,     "block_metadata",           "servux",   "blocks");
        //register(PayloadType.SERVUX_ENTITIES,   "entity_provider",          "servux",   "entities");
        //register(PayloadType.SERVUX_LITEMATICS, "litematic_shared_storage", "servux",   "litematics");
        //register(PayloadType.SERVUX_METADATA,   "metadata_service",         "servux",   "metadata");
        register(PayloadType.SERVUX_STRUCTURES, "structure_bounding_boxes", "servux",   "structures");

        // Debugging call
        //listTypes();
    }

    /**
     * Forces a reset() signal on all registered payloads
     */
    public void resetPayloads()
    {
        MaLiLib.printDebug("PayloadTypeRegister#resetPayloads(): sending reset() to all registered Payload types.");
        //listTypes();

        for (PayloadType type : TYPES.keySet())
        {
            if (TYPES.get(type).isPlayRegistered())
            {
                if (MaLiLibReference.isClient())
                    ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).reset(type);
                if (MaLiLibReference.isServer() || MaLiLibReference.isOpenToLan() || MaLiLibReference.isIntegrated() || MaLiLibReference.isDedicated())
                    ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).reset(type);
            }
            if (TYPES.get(type).isConfigRegistered())
            {
                if (MaLiLibReference.isClient())
                    ((ClientConfigHandler<?>) ClientConfigHandler.getInstance()).reset(type);
                if (MaLiLibReference.isServer() || MaLiLibReference.isOpenToLan() || MaLiLibReference.isIntegrated() || MaLiLibReference.isDedicated())
                    ((ServerConfigHandler<?>) ServerConfigHandler.getInstance()).reset(type);
            }
        }
    }
    /**
     * Forces a Type Handler Registration signal on all registered payloads
     * This is how data() fur babies are made.
     */
    public void registerAllHandlers()
    {
        MaLiLib.printDebug("PayloadTypeRegister#registerAllHandlers(): sending registerHandlers() to all registered Payload types.");
        //listTypes();

        for (PayloadType type : TYPES.keySet())
        {
            if (TYPES.get(type).isPlayRegistered())
            {
                if (MaLiLibReference.isClient())
                    ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).registerPlayHandler(type);
                if (MaLiLibReference.isServer() || MaLiLibReference.isOpenToLan() || MaLiLibReference.isIntegrated() || MaLiLibReference.isDedicated())
                    ((ServerPlayHandler<?>) ServerPlayHandler.getInstance()).registerPlayHandler(type);
            }
            if (TYPES.get(type).isConfigRegistered())
            {
                if (MaLiLibReference.isClient())
                    ((ClientConfigHandler<?>) ClientConfigHandler.getInstance()).registerConfigHandler(type);
                if (MaLiLibReference.isServer() || MaLiLibReference.isOpenToLan() || MaLiLibReference.isIntegrated() || MaLiLibReference.isDedicated())
                    ((ServerConfigHandler<?>) ServerConfigHandler.getInstance()).registerConfigHandler(type);
            }
        }
    }
    // For Debugging only
    public void listTypes()
    {
        for (PayloadCodec codec : TYPES.values())
        {
            MaLiLib.printDebug("listTypes(): type {} // {}", codec.getType().toString(), codec.getId().toString());
        }
    }
}