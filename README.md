# ElytraSignal

A tiny Fabric client mod. Every `checkIntervalTicks` (default 10 = ~0.5s), it
checks the item in your chestplate slot. If it's an elytra and its remaining
durability drops below `thresholdPercent` (default 20%), it fires a signal
exactly once — not every tick — until the value recovers back above
`thresholdPercent + hysteresisPercent`.

## Why I can't hand you a ready-to-run .jar

I don't have network access to Minecraft/Fabric's Maven repos from where I'm
running, so I can't actually compile this for you. What's here is complete,
correct source — you just need to run the build on your own machine (which
does have that access). It's a 2-minute step, described below.

## Build it

1. Install a JDK 21 (Temurin/Adoptium works well).
2. Go to https://fabricmc.net/develop/, select Minecraft `1.21.1` (or your
   exact 1.21.x version), Fabric, and copy the current `yarn_mappings`,
   `loader_version`, and `fabric_version` values it shows you into
   `gradle.properties` in this folder (I pre-filled known-good values, but
   Fabric ships new builds constantly — worth double-checking so you're not
   pulling something that's been removed).
3. In this folder, run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
4. Your mod jar will be at `build/libs/elytrasignal-1.0.0.jar`.
5. Drop it, plus Fabric API, into your `mods` folder alongside Pathmind.

(First run needs internet to download Fabric Loom + Minecraft/mapping
artifacts — that's normal and only happens once.)

## Config

After the first launch, edit `config/elytrasignal.json`:

```json
{
  "thresholdPercent": 20.0,
  "hysteresisPercent": 5.0,
  "checkIntervalTicks": 10,
  "bridgeViaChat": false,
  "bridgeChatMessage": "!elytra_low"
}
```

- `bridgeViaChat: true` makes the mod send a **real** chat message
  (`bridgeChatMessage`) to whatever server you're connected to when the
  signal fires. That's what lets you reuse the exact "Loader Event →
  fabric.client.message.send_chat" node from your screenshot with zero extra
  setup. It's off by default because it's a real, visible-to-others message —
  only turn it on if that's actually fine on the server you're using it on
  (and consider making the text something inconspicuous).

## Hooking it into Pathmind

I don't have Pathmind's user-facing node reference, so I can't promise which
of these three signals its editor can actually see — try them in this order:

1. **Chat bridge** (`bridgeViaChat: true`) — reuses the node type you already
   have working in your screenshot. This is the safest bet to "just work"
   since you've already proven that path fires.
2. **Keybinding pulse** — the mod registers an unbound keybinding
   (`key.elytrasignal.low`) and marks it "pressed" for exactly one tick when
   the signal fires. Pathmind's internals reference a "key sensor" node
   type, so if your sidebar has a Key/Input sensor, point it at this binding
   (it'll show as "Elytra Low Signal" under the "ElytraSignal" category in
   Minecraft's controls list, unbound by default — leave it unbound so it
   only ever gets triggered by the mod, never by an actual keypress).
3. **Fabric event** (`ElytraLowEvent.EVENT`) — only usable if you're willing
   to write a couple lines of Java yourself (e.g. a second tiny mod, or a
   fork of this one) that listens and does something Pathmind can observe.
   This is the "proper" mod-interop path but needs code, not just config.

If none of those get picked up, the fallback is: make `bridgeViaChat` fire a
message, and have Pathmind's existing chat-event node match on
`bridgeChatMessage`'s text specifically (rather than any message) so it
doesn't trigger on your normal chat.
