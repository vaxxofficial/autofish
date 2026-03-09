# AutoFish Mod

A simple, feature-rich Minecraft mod built for the Fabric loader (version 1.21.11) that automatically casts and reels in your fishing rod. It includes configurable randomization to mimic human behavior, specific enhancements to avoid AFK detection, and support for mythical fishing for Hypixel main lobby fishing.

The entirety of this mod was coded using AI. Claude's Sonnet 4.6 built the foundation and Gemini's 3.1 Pro built everything else. This code is almost certainly a mess, but it manages to run. Although I would not recommend changing the default timing settings, you are technically able to. This project took ~12 hours to create the initial commit, and is "my" first ever attempt to create a useful mod. Many aspects of this mod are miticuously thought over as to make the user appear as human as possible, but many aspects also are **not**. Use at your own risk.

## Features

- **Automated Fishing:** Re-casts and reels in your rod continuously as soon as a fish is caught.
- **Configurable Timings:** Allows setting minimum and maximum delays for reaction time and recasting to mimic human behavior and avoid automated detection.
- **Anti-AFK Movement (Walk & Jump):** Periodically moves your character left & right or jumps to prevent you from being kicked for inactivity. These options are mutually exclusive.
- **Mythical Fish Support:** Attempts to automatically catch mythical fish.
- **Ghosted Rod Protection:** Built-in settling timeouts to automatically fix client-server desyncs (ghosted rods) so you never get stuck.
- **Debug Mode:** A built-in chat command to see exactly what state the mod is in behind the scenes.
- **In-Game Configuration:** Includes an easy-to-use configuration menu powered by Cloth Config and Mod Menu.

## Installation

### Requirements
- Minecraft `1.21.11`
- Fabric Loader `>=0.17.0`
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Mod Menu](https://modrinth.com/mod/modmenu) (Recommended, for configuring the mod in-game)
- [Cloth Config API](https://modrinth.com/mod/cloth-config) (Included via jar-in-jar)

### Setup
1. Download the latest release `.jar` file.
2. Place the `.jar` into your `.minecraft/mods` folder.
3. Make sure you also have the Fabric API and Mod Menu `.jar` files in your `mods` folder.
4. Launch the game using your Fabric profile.

## Usage

### Commands
* **`/autofish debug`**: Toggles debug mode on/off. When on, it will print state changes to your chat to help diagnose issues.

### Default Keybinds
* **`P`**: Toggle AutoFish On/Off. You will receive an in-game message confirming the state.
* **`O`**: Open the AutoFish Settings menu.

*Note: You must have a fishing rod in your main hand or off-hand for the mod to start working.*

### Settings
The configuration menu provides several options to tailor your fishing experience:

* **Random Movement:** Randomly moves your character left and right to avoid AFK detection. Turns off Jump Movement if enabled.
* **Jump Movement:** Randomly jumps to avoid AFK detection. Turns off Random Movement if enabled.
* **Catch Mythical:** Specifically tracks and attempts to catch mythical fish (useful on servers like Hypixel).
* **Min/Max Reaction (ticks):** The delay between the bobber dipping and the rod reeling in.
* **Min/Max Recast (ticks):** The delay between reeling in and casting the rod out again.

## License
This project is licensed under the MIT License.
