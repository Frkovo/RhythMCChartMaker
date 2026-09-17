package cn.frkovo.rhythmcv2.rmcChart.client.editor.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side editor camera controller.
 *
 * <p>Drives a dummy armor stand (never added to the world) and focuses the
 * vanilla camera on it via {@code MinecraftClient.setCameraEntity}. Modes:
 * PLAYER restores the normal player camera; FREE is a fly camera moved with
 * WASD while the editor shell is open (the Screen owns gameplay input); TOP and
 * FRONT are fixed views around the captured world anchor.
 */
public final class EditorCameraController {
    public enum Mode {
        PLAYER("Player"),
        FREE("Free"),
        TOP("Top"),
        FRONT("Front");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final double FREE_SPEED_PER_TICK = 0.4;
    private static final double TOP_HEIGHT = 18.0;
    private static final double FRONT_DISTANCE = 8.0;

    private static Mode mode = Mode.PLAYER;
    private static ArmorStandEntity dummy;
    private static double camX;
    private static double camY;
    private static double camZ;
    private static float camYaw;
    private static float camPitch;

    private EditorCameraController() {
    }

    public static Mode mode() {
        return mode;
    }

    public static boolean isDetached() {
        return mode != Mode.PLAYER;
    }

    public static void cycleMode(MinecraftClient client) {
        setMode(client, mode.next());
    }

    public static void setMode(MinecraftClient client, Mode newMode) {
        if (client == null || client.world == null || client.player == null) {
            return;
        }
        if (newMode == mode && (newMode == Mode.PLAYER || dummy != null)) {
            return;
        }
        mode = newMode;
        if (mode == Mode.PLAYER) {
            detach(client);
            return;
        }
        ensureDummy(client);
        switch (mode) {
            case FREE -> {
                Vec3d eye = client.player.getEyePos();
                camX = eye.x;
                camY = eye.y;
                camZ = eye.z;
                camYaw = client.player.getYaw();
                camPitch = client.player.getPitch();
            }
            case TOP -> {
                camX = EditorWorldPlacement.anchorX();
                camY = EditorWorldPlacement.anchorY() + TOP_HEIGHT;
                camZ = EditorWorldPlacement.anchorZ();
                camYaw = 0.0f;
                camPitch = 90.0f; // MC pitch: +90 looks straight down.
            }
            case FRONT -> {
                camX = EditorWorldPlacement.anchorX();
                camY = EditorWorldPlacement.anchorY() + 1.6;
                camZ = EditorWorldPlacement.anchorZ() + FRONT_DISTANCE;
                camYaw = 180.0f;
                camPitch = 0.0f;
            }
            default -> {
            }
        }
        pushToDummy(client);
    }

    public static void tick(MinecraftClient client) {
        if (mode == Mode.PLAYER) {
            return;
        }
        if (client == null || client.world == null || client.player == null) {
            return;
        }
        ensureDummy(client);
        if (dummy == null) {
            return;
        }
        if (mode == Mode.FREE) {
            camYaw = client.player.getYaw();
            camPitch = client.player.getPitch();
            // Fly input only while a screen owns gameplay input; otherwise WASD
            // would move both the player and the camera.
            if (client.currentScreen != null && client.getWindow() != null
                    && !(client.currentScreen.getFocused() instanceof TextFieldWidget)) {
                Window window = client.getWindow();
                double forward = axis(window, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_S);
                double strafe = axis(window, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_A);
                double vertical = axis(window, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT);
                if (forward != 0.0 || strafe != 0.0 || vertical != 0.0) {
                    double yawRad = Math.toRadians(camYaw);
                    double sin = Math.sin(yawRad);
                    double cos = Math.cos(yawRad);
                    // Vanilla movementInputToVelocity basis:
                    // forward = (-sin, 0, cos), right = (cos, 0, sin).
                    camX += (-sin * forward + cos * strafe) * FREE_SPEED_PER_TICK;
                    camZ += (cos * forward + sin * strafe) * FREE_SPEED_PER_TICK;
                    camY += vertical * FREE_SPEED_PER_TICK;
                }
            }
        }
        pushToDummy(client);
    }

    private static double axis(Window window, int positive, int negative) {
        boolean pos = InputUtil.isKeyPressed(window, positive);
        boolean neg = InputUtil.isKeyPressed(window, negative);
        if (pos == neg) {
            return 0.0;
        }
        return pos ? 1.0 : -1.0;
    }

    private static void ensureDummy(MinecraftClient client) {
        if (dummy == null || dummy.getEntityWorld() != client.world) {
            dummy = new ArmorStandEntity(EntityType.ARMOR_STAND, client.world);
        }
    }

    private static void pushToDummy(MinecraftClient client) {
        if (dummy == null) {
            return;
        }
        dummy.setPos(camX, camY - dummy.getStandingEyeHeight(), camZ);
        dummy.setYaw(camYaw);
        dummy.setPitch(camPitch);
        dummy.lastRenderX = dummy.getX();
        dummy.lastRenderY = dummy.getY();
        dummy.lastRenderZ = dummy.getZ();
        dummy.lastYaw = camYaw;
        dummy.lastPitch = camPitch;
        dummy.setHeadYaw(camYaw);
        dummy.lastHeadYaw = camYaw;
        if (client.getCameraEntity() != dummy) {
            client.setCameraEntity(dummy);
        }
    }

    /** Restores the player camera and drops the dummy. Safe to call when already restored. */
    public static void detach(MinecraftClient client) {
        if (client != null && client.player != null && client.getCameraEntity() != client.player) {
            client.setCameraEntity(client.player);
        }
        dummy = null;
        mode = Mode.PLAYER;
    }

    /** Restores the player camera but keeps the current mode for re-entry. */
    public static void suspend(MinecraftClient client) {
        if (client != null && client.player != null && client.getCameraEntity() != client.player) {
            client.setCameraEntity(client.player);
        }
    }
}
