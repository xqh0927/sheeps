# Multiplayer Spell System Design Specification

This specification documents the design of the Active Spell/Prank system added to the multiplayer mode of the game.

## Overview
The active spell system allows players to accumulate energy during matching/elimination and unleash real-time curses against the opponent to alter their board difficulty and rules dynamically.

---

## 1. Data Protocol (`GameCommand.kt` & `CommandPayload`)

### CommandType Extension
Add `CAST_SPELL` to `CommandType`:
```kotlin
enum class CommandType {
    ELIMINATE,
    ATTACK,
    BUFF,
    RECONNECT_RESUME,
    SYSTEM_EVENT,
    CAST_SPELL
}
```

### CommandPayload Extension
Add `spellType` and `spellDuration` to `CommandPayload`:
```kotlin
@Serializable
data class CommandPayload(
    val tilesEliminated: List<String>? = null,
    val comboCount: Int = 0,
    val attackPower: Double = 0.0,
    val obstacleType: String? = null,
    val targetPlayerId: String? = null,
    val activeBuffId: String? = null,
    val systemMessage: String? = null,
    val spellType: String? = null,     // "FOG", "SHRINK", "SEAL_ALL"
    val spellDuration: Long = 0L       // duration in milliseconds
)
```

---

## 2. Server Broker
The Cloudflare Worker accepts `CAST_SPELL` messages and inserts them into the `game_commands` table on D1 database, forwarding them to the opponent's polling WebSocket instance.

---

## 3. Client state and UI

### DuelViewState Additions
```kotlin
data class DuelViewState(
    // ... Existing properties
    val currentEnergy: Int = 0,
    val isFogActive: Boolean = false,
    val maxSlotSize: Int = 7,
    val activeSpellMessage: String? = null
)
```

### Eliminating Cards -> Energy Bar
Whenever a group of cards is matched and eliminated (`processSlotMatch`), `currentEnergy` increases by 1 (capped at 10).

### Spell panel buttons
Three spell buttons below the cards slots:
1. **Fog (3 energy)**: Sends `CAST_SPELL` with type `"FOG"`, duration `6000` ms.
2. **Shrink (6 energy)**: Sends `CAST_SPELL` with type `"SHRINK"`, duration `8000` ms.
3. **Seal All (10 energy)**: Sends `CAST_SPELL` with type `"SEAL_ALL"`.

### Curse Application (Opponent)
1. **FOG**: `isFogActive` is set to `true` for 6 seconds.
   - Screen overlay: A dark gray alpha-blended Canvas mask covers the cards.
   - Interaction: Touch events reveal a circular region around the pointer (radius 50dp).
2. **SHRINK**: `maxSlotSize` becomes `6` for 8 seconds.
   - UI: Slot cell 7 displays a red lock.
   - Game logic: If the slot has >= 6 tiles without a match, the game immediately ends in defeat.
3. **SEAL_ALL**: All board cards in `TileState.NORMAL` state (exposed and clickable) have `sealedCount` increased by 1. Recalculate blocking states.
