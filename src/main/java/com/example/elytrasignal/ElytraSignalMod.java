package com.example.elytrasignal;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElytraSignalMod implements ClientModInitializer {
    public static final String MOD_ID = "elytrasignal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ElytraSignalConfig config;

    /**
     * Unbound by default (GLFW_KEY_UNKNOWN). This binding is never meant to be
     * pressed by a human at the keyboard - it exists purely so external tools
     * (Pathmind key-sensor nodes, macro tools, etc.) that can watch a
     * KeyBinding's pressed state have something to watch. We flip it to
     * "pressed" for exactly one tick when the signal fires.
     */
    private static KeyBinding lowDurabilitySignalKey;

    private int tickCounter = 0;
    private boolean currentlyLow = false;

    @Override
    public void onInitializeClient() {
        config = ElytraSignalConfig.load();

        lowDurabilitySignalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytrasignal.low",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.elytrasignal"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        LOGGER.info("ElytraSignal initialized. Threshold: {}% (checking every {} ticks)",
                config.thresholdPercent, config.checkIntervalTicks);
    }

    private void onTick(MinecraftClient client) {
        // Always release the signal key one tick after we set it, so it reads as a
        // single "press" rather than being held down.
        if (lowDurabilitySignalKey.isPressed()) {
            lowDurabilitySignalKey.setPressed(false);
        }

        if (client.player == null || client.world == null) {
            return;
        }

        tickCounter++;
        if (tickCounter < config.checkIntervalTicks) {
            return;
        }
        tickCounter = 0;

        ItemStack chest = client.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !chest.isOf(Items.ELYTRA)) {
            currentlyLow = false; // no elytra equipped; reset state
            return;
        }

        int maxDamage = chest.getMaxDamage();
        if (maxDamage <= 0) {
            return; // unbreakable elytra, nothing to signal
        }
        int damage = chest.getDamage();
        double remainingPercent = 100.0 * (maxDamage - damage) / maxDamage;

        if (!currentlyLow && remainingPercent < config.thresholdPercent) {
            currentlyLow = true;
            fireSignal(client, remainingPercent);
        } else if (currentlyLow && remainingPercent >= config.thresholdPercent + config.hysteresisPercent) {
            currentlyLow = false; // recovered (repaired/swapped), allow it to fire again later
        }
    }

    private void fireSignal(MinecraftClient client, double remainingPercent) {
        LOGGER.info("Elytra durability low: {}%", String.format("%.1f", remainingPercent));

        // 1. Fabric event - any mod can listen directly.
        ElytraLowEvent.EVENT.invoker().onElytraLow(remainingPercent);

        // 2. Keybinding pulse - for tools that can sense a KeyBinding's pressed state.
        lowDurabilitySignalKey.setPressed(true);

        // 3. Optional real chat message bridge.
        if (config.bridgeViaChat && client.player != null && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatMessage(config.bridgeChatMessage);
        }
    }
}
