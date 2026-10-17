package com.miku.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public class MikuClient implements ClientModInitializer {
    private static final int CYAN = 0xFF39E6D0;
    private static final int PINK = 0xFFFF82C8;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL = 0xC9101825;

    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();

    private static boolean hudEnabled = true;
    private static boolean wasLeftDown;
    private static boolean wasRightDown;

    private KeyBinding toggleHud;

    @Override
    public void onInitializeClient() {
        toggleHud = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.mikuclient.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.mikuclient"
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHud.wasPressed()) {
                hudEnabled = !hudEnabled;
            }

            if (client.player == null || client.getWindow() == null) {
                leftClicks.clear();
                rightClicks.clear();
                wasLeftDown = false;
                wasRightDown = false;
                return;
            }

            if (client.currentScreen != null) {
                wasLeftDown = false;
                wasRightDown = false;
                return;
            }

            long window = client.getWindow().getHandle();
            boolean left = GLFW.glfwGetMouseButton(
                window, GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS;
            boolean right = GLFW.glfwGetMouseButton(
                window, GLFW.GLFW_MOUSE_BUTTON_RIGHT
            ) == GLFW.GLFW_PRESS;

            long now = System.currentTimeMillis();

            if (left && !wasLeftDown) {
                leftClicks.addLast(now);
            }
            if (right && !wasRightDown) {
                rightClicks.addLast(now);
            }

            wasLeftDown = left;
            wasRightDown = right;

            trimClicks(leftClicks, now);
            trimClicks(rightClicks, now);
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (!hudEnabled || client.player == null
                    || client.options.hudHidden) {
                return;
            }

            renderHud(context, client);
        });
    }

    private static void trimClicks(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty()
                && now - clicks.peekFirst() >= 1000) {
            clicks.removeFirst();
        }
    }

    private static void renderHud(
            DrawContext ctx, MinecraftClient client) {
        int x = 8;
        int y = 8;

        panel(ctx, x, y, 174, 99);

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal("MIKU CLIENT"),
            x + 7, y + 6, CYAN
        );

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal("FPS: " + client.getCurrentFps()),
            x + 7, y + 22, WHITE
        );

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal(String.format(
                "XYZ: %.1f / %.1f / %.1f",
                client.player.getX(),
                client.player.getY(),
                client.player.getZ()
            )),
            x + 7, y + 35, WHITE
        );

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal(
                "L-CPS: " + leftClicks.size()
                + "   R-CPS: " + rightClicks.size()
            ),
            x + 7, y + 48, CYAN
        );

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal("HATSUNE MIKU"),
            x + 7, y + 63, PINK
        );

        ctx.drawTextWithShadow(
            client.textRenderer,
            Text.literal("RIGHT SHIFT: HUD"),
            x + 7, y + 80, 0xFFB8C7D9
        );

        drawKeys(ctx, client, 8, 113);
    }

    private static void drawKeys(
            DrawContext ctx, MinecraftClient client,
            int x, int y) {
        panel(ctx, x, y, 77, 57);

        key(ctx, client, "W", x + 28, y + 4,
            client.options.forwardKey.isPressed());
        key(ctx, client, "A", x + 9, y + 22,
            client.options.leftKey.isPressed());
        key(ctx, client, "S", x + 28, y + 22,
            client.options.backKey.isPressed());
        key(ctx, client, "D", x + 47, y + 22,
            client.options.rightKey.isPressed());
        key(ctx, client, "SPC", x + 20, y + 39,
            client.options.jumpKey.isPressed());
    }

    private static void panel(
            DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, PANEL);
        ctx.fill(x, y, x + w, y + 2, CYAN);
    }

    private static void key(
            DrawContext ctx, MinecraftClient client,
            String label, int x, int y, boolean pressed) {
        ctx.fill(
            x, y, x + 17, y + 16,
            pressed ? 0xFF168F88 : 0xFF263746
        );

        ctx.drawCenteredTextWithShadow(
            client.textRenderer,
            Text.literal(label),
            x + 8, y + 4, WHITE
        );
    }
}
