# Changelog - Soulbound Gravestones

All notable changes to this fork of Gravestones will be documented in this file.

## [2.0.0] - 2025-10-13

### Added - Major Features

#### 🔑 Soulbound Gravestone Key System
- **Gravestone Key Item**: A magical key that drops automatically on death
  - Cannot be dropped, thrown, or placed in containers (soulbound)
  - Hold right-click to charge and teleport to your most recent gravestone
  - Configurable charge time (default: 1.6 seconds, matching Waystones)
  - Particle effects and sound feedback during charging
  - Auto-consumed after successful teleportation
  - Config option to completely disable key dropping (`drop_gravestone_key`)
  - Config option to adjust teleport delay (`gravestone_key_teleport_delay`)

#### 💀 Gravestone Curse Effect
- **Custom Status Effect**: "Maldición de la Tumba" (Gravestone Curse)
  - Displays as a single effect with a key icon in the inventory
  - **Slowness III** (45% movement speed reduction) for 5 seconds
  - **Blindness** effect for 5 seconds with authentic vanilla fog and darkness overlay
  - **Hunger Drain**: Consumes up to 4 hunger points (2 hunger bars) when opening a grave
  - Effects are replicated client-side without showing vanilla effect icons
  - Perfect visual parity with vanilla blindness using custom mixins

#### 📜 Gravestone History & Recovery System
- **Persistent History Storage**: Automatically saves your last 20 gravestones
  - Stores complete gravestone data (contents, location, owner, death time)
  - Survives server restarts and crashes
  - JSON-based storage in `world/data/gravestones/history/`
  
- **Interactive History GUI**: View and manage your gravestone history
  - Command: `/gravestones history list`
  - Displays gravestone details (owner, location, death time)
  - **[View]** button: Preview gravestone contents in a chest-like interface
  - **[Spawn]** button: Recreate the gravestone at your current location
  - Color-coded entries (gold for viewing, green for spawning)
  - Pagination support for large histories
  
- **Manual Recovery Command**:
  - Command: `/gravestones history restore <index>`
  - Spawns selected gravestone at your current location
  - Validates gravestone data before spawning
  - Feedback messages for success/failure

#### 🧰 Enhanced Accessories Integration
- Full compatibility with the [Accessories mod](https://modrinth.com/mod/accessories)
- Accessories are properly stored in gravestones and restored on retrieval
- Maintains all accessory data and NBT tags

#### 🛡️ Security & Quality of Life Improvements
- **Key Drop Prevention**: Gravestone keys cannot be accidentally dropped or thrown
  - Mixin prevents key from being dropped via Q key
  - Mixin prevents key from being thrown by player
  - Keys remain safe even in multiplayer environments
  
- **Command Restructuring**: Complete overhaul of command system
  - Organized into logical subcommands: `/gravestones history <list|restore>`
  - Improved error messages and user feedback
  - Better permission handling
  - Cleaner code structure using Fabric's command API

- **Attribution & Documentation**:
  - Updated all headers with proper fork attribution
  - Maintained original author credits
  - Clear distinction between original features and fork additions
  - Comprehensive README.md with feature documentation

### Technical Changes

#### Client-Side Rendering
- **BackgroundRendererMixin**: Replicates vanilla blindness fog effect
  - Injects into `applyFog` method to set fog distance
  - Uses exact vanilla fog values (start: -4.0, end: -2.0)
  
- **InGameHudMixin**: Renders darkness overlay for blindness
  - Injects into HUD render method
  - Semi-transparent black overlay (50% alpha)
  - Synchronized with curse effect duration

#### Game Mechanics
- **GravestoneCurseStatusEffect**: Custom status effect implementation
  - Harmful category with dark purple color (#2C1B47)
  - Attribute modifier for movement speed reduction
  - No visible particle effects
  - Key icon in inventory
  
- **PreventKeyDropMixin**: Prevents accidental key loss
  - Intercepts player item drop events
  - Blocks Q key drops for gravestone keys
  - Prevents throw/toss actions

- **LivingEntityMixin**: Ensures keys stay with player
  - Intercepts drop item on death
  - Prevents keys from being dropped as items

#### Data Management
- **GravestoneHistoryIO**: JSON-based persistent storage
  - Circular buffer with 20-entry limit
  - Atomic file writes with backup
  - Automatic cleanup of old entries
  
- **RecentGraveHistory**: In-memory history tracking
  - Per-player history management
  - Thread-safe operations
  - Efficient lookups and updates

#### Commands
- Restructured command system with subcommands
- `/gravestones history list`: Opens interactive GUI
- `/gravestones history restore <index>`: Manual restoration
- Improved error handling and feedback

### Configuration Options (New)
```properties
# Drop gravestone key on death (true/false)
drop_gravestone_key=true

# Teleport delay in seconds when using gravestone key
gravestone_key_teleport_delay=1.6
```

### Dependencies
- **Added**: Accessories API (optional, for enhanced compatibility)
- **Maintained**: PneumonoCore (required)
- **Maintained**: Fabric API (required)

### Compatibility
- Minecraft 1.21 - 1.21.1
- Fabric Loader >=0.16.14
- Java 21+

### Known Issues
None at this time.

### Credits
- **Original Mod**: [Pneumono_](https://github.com/PneumonoIsNotAvailable) - Creator of Gravestones
- **Fork Development**: IgnacioDX - All enhanced features and improvements
- **Original Contributors**: EastMonster, dirtTW, wouter173, StarmanMine142

---

## [1.x.x] - Original Gravestones
See [original mod changelog](https://github.com/PneumonoIsNotAvailable/Gravestones) for base features.
