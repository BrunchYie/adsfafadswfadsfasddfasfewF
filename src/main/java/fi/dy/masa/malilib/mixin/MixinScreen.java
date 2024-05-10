package fi.dy.masa.malilib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * This stops Minecraft from crashing when resizing the Minecraft Window, when the MaLiLib rendering is active,
 * because this.client == null
 */
@Mixin(Screen.class)
public class MixinScreen
{
    @Shadow
    protected MinecraftClient client;

    @Inject(method = "setInitialFocus()V", at = @At("HEAD"))
    private void malilib_screenResizeFix(CallbackInfo ci)
    {
        if (this.client == null)
        {
            this.client = MinecraftClient.getInstance();
        }
    }
}
