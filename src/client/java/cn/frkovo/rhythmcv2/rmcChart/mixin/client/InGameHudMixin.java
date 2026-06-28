package cn.frkovo.rhythmcv2.rmcChart.mixin.client;

import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
abstract class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void rmcChart$renderEditorHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        EditorHudRenderer.render(context);
    }
}
