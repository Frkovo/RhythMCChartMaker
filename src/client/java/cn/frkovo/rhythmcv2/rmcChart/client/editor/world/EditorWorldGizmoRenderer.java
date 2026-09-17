package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator.NoteState;
import cn.frkovo.rhythmcv2.rmcChart.chart.core.ChartEvaluator.TrackState;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.ChartEditorScreen;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.EditorSelection;
import cn.frkovo.rhythmcv2.rmcChart.client.editor.model.EditorTool;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/**
 * Draws an axis cross on the selected note while the TRANSFORM tool is active.
 * All emitted vertices are camera-relative, per the Fabric world rendering API contract.
 */
public final class EditorWorldGizmoRenderer {
    private static final float AXIS_HALF = 0.6f;

    private EditorWorldGizmoRenderer() {
    }

    public static void endMain(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof ChartEditorScreen screen)) {
            return;
        }
        if (screen.state().activeTool() != EditorTool.TRANSFORM) {
            return;
        }
        EditorSelection selection = screen.state().selection();
        if (selection == null || selection.kind() != EditorSelection.Kind.NOTE
                || selection.track() == null || selection.note() == null) {
            return;
        }
        if (!EditorWorldPlacement.hasAnchor()) {
            return;
        }
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }

        TrackState trackState = ChartEvaluator.evaluateTrack(selection.track(),
                screen.state().level().meta().bpms(), screen.state().playheadBeat());
        NoteState noteState = ChartEvaluator.evaluateNote(trackState,
                screen.state().level().meta().bpms(), selection.note());
        Vec3d center = EditorWorldPlacement.chartToWorld(noteState.worldX(), noteState.worldY(), noteState.worldZ());
        Vec3d camPos = camera.getCameraPos();
        Vec3d normal = camPos.subtract(center).normalize();

        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());
        matrices.push();
        matrices.translate(center.x - camPos.x, center.y - camPos.y, center.z - camPos.z);
        MatrixStack.Entry entry = matrices.peek();
        // World axes: X red, Y green, Z blue. Chart z maps to world -z.
        line(lines, entry, -AXIS_HALF, 0, 0, AXIS_HALF, 0, 0, 0xFFFF5555, normal);
        line(lines, entry, 0, -AXIS_HALF, 0, 0, AXIS_HALF, 0, 0xFF55FF55, normal);
        line(lines, entry, 0, 0, -AXIS_HALF, 0, 0, AXIS_HALF, 0xFF5599FF, normal);
        matrices.pop();
    }

    private static void line(VertexConsumer lines, MatrixStack.Entry entry,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             int argb, Vec3d normal) {
        float nx = (float) normal.x;
        float ny = (float) normal.y;
        float nz = (float) normal.z;
        lines.vertex(entry, (float) x1, (float) y1, (float) z1).color(argb).normal(entry, nx, ny, nz);
        lines.vertex(entry, (float) x2, (float) y2, (float) z2).color(argb).normal(entry, nx, ny, nz);
    }
}
