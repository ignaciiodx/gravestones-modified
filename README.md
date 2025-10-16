# Soulbound Gravestones

> **Enhanced Fork of [Gravestones](https://modrinth.com/project/Heh3BbSv) by [PneumonoIsNotAvailable](https://github.com/PneumonoIsNotAvailable)**
> 
> This is a modified version that adds a soulbound key system, complete gravestone history & recovery, and enhanced mod compatibility. All original features and credits belong to the original author Pneumono_.

## About This Version

**Version 2.0.2 for Minecraft 1.21.1**

Soulbound Gravestones is an enhanced version of the original Gravestones mod that adds blocks which store your items on death. This release (2.0.2) has fixed players keeping the key if not used after opening a gravestone.

### ✨ What's New in Soulbound Gravestones

🔑 **Soulbound Gravestone Key**: A magical key that drops when you die, allowing you to teleport back to your gravestone
- Hold right-click to charge the key (configurable delay with particles and sound effects)
- Teleports you directly to your most recent gravestone
- Cannot be dropped or thrown away - it's bound to your soul
- Key behavior can be disabled or customized in the config

� **Gravestone Curse**: Opening a gravestone applies a powerful curse effect
- **Slowness III** (45% movement speed reduction) for 5 seconds
- **Blindness** effect (reduced visibility and fog) for 5 seconds
- **Hunger Drain**: Consumes up to 4 hunger points (2 hunger bars)
- The curse is represented by a single custom status effect with a key icon


�📜 **Gravestone History & Recovery System**: Never lose your items to bugs or accidents again
- Automatically stores your last 20 gravestones with all their data
- Use `/gravestones history list` to view your gravestone history with an interactive GUI
- Click **[View]** to preview gravestone contents or **[Spawn]** to restore it at your location
- Use `/gravestones history restore <index>` to recreate a lost gravestone
- Perfect for recovering from server crashes, bugs, or accidental destruction

🧰 **Accessories Integration**: Full compatibility and support for the [Accessories mod](https://modrinth.com/mod/accessories), ensuring accessory items are properly stored in gravestones

⚙️ **Enhanced Configuration**:
- Toggle gravestone key on/off
- Adjust teleportation charge time
- All original configuration options remain available

### Original Mod Features

The base Gravestones mod includes all the following features that remain unchanged:

### Decay
Gravestone decay is very configurable and **all of its features can be configured or disabled** if they are not wanted!

By default, all gravestones decay over time and due to subsequent deaths. This is shown visually by a change in the block's texture.



After 3 stages of decay, the gravestone breaks entirely, spilling its contents onto the ground (after which the vanilla 5-minute despawn timer applies).

### Aesthetic Gravestones
Gravestones also includes craftable "aesthetic gravestones", which allow builders to use the gravestone blocks without having their builds at risk of damage.
They can also be written on and dyed just like signs.


### Configs
This is a short summary of some of the mod's configs. For the full list with more detailed explanations, see [the wiki](https://github.com/PneumonoIsNotAvailable/Gravestones/wiki/Configs).
- Whether gravestones decay due to subsequent deaths
- Whether gravestones decay over time, and how long it takes
- Whether gravestones store experience, how much they store, and whether experience "decays" over time along with the gravestone itself
- Whether gravestones can be accessed by players other than their owner
- What time format the gravestones display (client-side)
- And more!

### Compatibility
This modified version has built-in support for:
- **Trinkets** - Original compatibility maintained  
- Several **Soulbound enchantment mods**
- **Spelunkery's recovery compass** changes

If another mod adds an item or enchantment with some kind of functionality on death that gravestones is affecting (e.g. an item that stays with the player on death, or something like Curse of Vanishing),
datapack/modpack/mod developers can add it to the tag `gravestones:skips_gravestones`, which makes items ignore gravestones entirely, and act as if the mod is not installed.

Mod developers can also add more complex conditions for gravestone skipping, if necessary.
Mods that add custom slots, or some other data that is dropped on death, can easily support for Gravestones by registering a gravestone data type.
A more detailed explanation of this can be found [here](https://github.com/PneumonoIsNotAvailable/Gravestones/wiki/Compatibility).

### Translations
At the time of writing, Gravestones currently only has full translations for English. Slightly outdated translations for simplified and traditional Chinese are available.

If you're able to translate Gravestones into a different language, please create a pull request and do so!
Create a copy of the `en_us.json` file in the mod's asset folder, and rename it to the appropriate locale code (viewable on the [Minecraft Wiki](https://minecraft.wiki/w/Language)).
Then simply go through all the English phrases and translate them.

### Dependencies
This enhanced version depends on:
- [PneumonoCore](https://modrinth.com/project/ZLKQjA7t) (required)
- [Accessories](https://modrinth.com/mod/accessories) (optional, for enhanced compatibility)

### Credits
- **Original Mod Author**: [Pneumono_](https://github.com/PneumonoIsNotAvailable) - Creator of [Gravestones](https://modrinth.com/project/Heh3BbSv)
- **Fork Author**: IgnacioDX ([ignaciiodx](https://github.com/ignaciiodx)) - Soulbound Gravestones enhancements
- **Original Contributors**: EastMonster, dirtTW, wouter173, StarmanMine142
- **New Features in This Fork**:
  - Soulbound gravestone key with teleportation mechanics
  - Complete gravestone history & recovery system with interactive GUI
  - Gravestone Curse: Opening gravestones applies Slowness III, Blindness, and drains hunger
  - Enhanced Accessories integration
  - Configurable key behavior and teleport delay
  - Prevention of accidental key dropping/throwing

### License
This project is licensed under the MIT License - same as the original mod.
- Original Gravestones: Copyright (c) 2023 Pneumono_
- Soulbound Gravestones: Copyright (c) 2025 IgnacioDX

See the [LICENSE](LICENSE) file for full details.

### Links
- **This Fork**: [Soulbound Gravestones on GitHub](https://github.com/ignaciiodx/Soulbound-Gravestones)
- **Original Mod**: [Gravestones on Modrinth](https://modrinth.com/project/Heh3BbSv) | [Source Code](https://github.com/PneumonoIsNotAvailable/Gravestones)
- **Dependencies**: [PneumonoCore](https://modrinth.com/project/ZLKQjA7t) | [Accessories](https://modrinth.com/mod/accessories)
