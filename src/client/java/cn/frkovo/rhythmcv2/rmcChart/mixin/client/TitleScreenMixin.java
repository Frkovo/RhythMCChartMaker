package cn.frkovo.rhythmcv2.rmcChart.mixin.client;

import cn.frkovo.rhythmcv2.rmcChart.client.RmcChartClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rmcChart$addCreateSongButton(CallbackInfo ci) {
        // MVP-DISABLED (2026-09-05): mod UI commented out, rewrite pending.
        // addDrawableChild(ButtonWidget.builder(Text.literal("RhythMC Chart Maker"), button -> RmcChartClient.connectToPreviewServer(this))
        //         .dimensions(this.width / 2 - 100, this.height / 4 + 180, 200, 20)
        //         .build());
    }
}
