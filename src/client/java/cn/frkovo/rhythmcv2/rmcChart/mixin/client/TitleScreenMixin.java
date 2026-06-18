package cn.frkovo.rhythmcv2.rmcChart.mixin.client;

import cn.frkovo.rhythmcv2.rmcChart.client.project.ProjectHubScreen;
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
        int buttonWidth = 140;
        int buttonX = 8;
        int buttonY = this.height - 28;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("RhythMC Projects"), button ->
                        this.client.setScreen(new ProjectHubScreen((TitleScreen) (Object) this)))
                .dimensions(buttonX, buttonY, buttonWidth, 20)
                .build());
    }
}
