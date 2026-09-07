package com.example.elytrasignal;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Fired exactly once when the equipped elytra's remaining durability
 * percentage drops below the configured threshold. Fires again only
 * after the percentage has recovered back above (threshold + hysteresis),
 * so it will not spam every tick while flying at low durability.
 *
 * Other mods (or reflection/mixin-based bridges) can listen with:
 *
 *   ElytraLowEvent.EVENT.register((percent) -> {
 *       // percent is 0.0-100.0, remaining durability
 *   });
 */
public final class ElytraLowEvent {
    private ElytraLowEvent() {}

    public interface Listener {
        void onElytraLow(double remainingPercent);
    }

    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(
            Listener.class,
            listeners -> (percent) -> {
                for (Listener listener : listeners) {
                    listener.onElytraLow(percent);
                }
            }
    );
}
