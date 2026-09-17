package cn.frkovo.rhythmcv2.rmcChart.mixin.client;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
abstract class GameMenuScreenMixin {
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void rmcChart$returnToEditorFromWorldPreview(CallbackInfo ci) {
        // MVP-DISABLED (2026-09-05): mod UI commented out, rewrite pending.
        // if (RmcChartClient.shouldReturnToEditorFromWorldPreview()) {
        //     RmcChartClient.returnToEditorFromWorldPreview();
        //     ci.cancel();
        // }
    }
}
