package cn.frkovo.rhythmcv2.rmcChart.client.editor;

import net.minecraft.client.gui.DrawContext;

final class AutomationCardUi {
    private AutomationCardUi() {
    }

    static String subtitle() {
        return "Single automation card  |  Start  End  From  To  Ease";
    }

    static String inspectorClickStatus() {
        return "Single automation card: edit values directly";
    }

    static String timelineLaneStatus(int trackId, String laneLabel) {
        return "Selected TrackID " + trackId + " event lane: single automation card (" + laneLabel + ")";
    }

    static void drawCardBackground(DrawContext context, int x, int y, int right) {
        context.fill(x, y, right, y + 18, 0x10161D24);
    }
}
