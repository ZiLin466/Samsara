<div align="center">
<p>
    <img width="200" src="src/main/resources/assets/samsara/icon.png" alt="Samsara logo">
</p>

<h1>Samsara Client</h1>
<p>B1 &middot; Minecraft 26.3 &middot; Fabric &middot; Java 25</p>

[License](#license) |
[Workspace](#setting-up-a-workspace) |
[Installation](#installation) |
[Getting Started](#getting-started)

</div>

Samsara is a free and open-source mixin-based hacked client for Minecraft, built on Fabric. It combines combat,
movement and player modules with a custom interface, configurable HUDs and local account management.

## License

Samsara is licensed under the [GNU General Public License v3.0](LICENSE).

You may use, modify and redistribute the project under GPLv3. When distributing a modified
version, you must provide its corresponding source code and license it under GPLv3.
Third-party dependencies and assets retain their respective licenses.

## Setting up a Workspace

Samsara uses the included [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).
Install a **JDK 25** and configure `JAVA_HOME` to point to it, then open a terminal in the repository root.
A separate Gradle installation is unnecessary.

```shell
git clone https://github.com/ZiLin466/Samsara.git
cd Samsara
```

1. Open the repository as a Gradle project in your preferred IDE and select JDK 25 as its Gradle JVM.
2. Generate Minecraft sources for easier code navigation (optional):

   ```powershell
   .\gradlew.bat genSources
   ```

3. Build the client and run the existing tests:

   ```powershell
   .\gradlew.bat build
   ```

   The client JAR is `build/libs/samsara-b1.jar`.

4. Start a development client when needed:

   ```powershell
   .\gradlew.bat runClient
   ```

On Linux or macOS, use `./gradlew` in place of `.\gradlew.bat`.
The first build downloads Gradle, Minecraft and the required dependencies.

JavaFX native libraries are selected for the build machine's operating system. When building
for another platform, set `-Psamsara.webViewPlatform=win`, `linux`, `mac` or `mac-aarch64` as appropriate.

## Installation

| Requirement | Version |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 or newer |
| Java runtime | 25 or newer |

1. Build the client using the workspace instructions above.
2. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3.
3. Copy `build/libs/samsara-b1.jar` into the `mods` folder of that Minecraft instance.
4. Launch the Fabric profile with the required Java runtime.

## Getting Started

- Press **Right Shift** to open ClickGUI with the default binding.
- In Opai, left-click a module to toggle it and right-click to expand its settings.
- Use the pencil button in ClickGUI to open the HUD editor.
- Open account settings from the main menu to manage saved accounts or switch sessions.
- Enter `.help` in chat to list commands. Save and load gameplay presets with
  `.config save <name>` and `.config load <name>`.

Automatic state is stored in `samsara/state.json`, and gameplay presets are stored in
`samsara/configs/`, relative to the active Minecraft game directory.

## Additional Libraries

### Mixins

[SpongePowered Mixin](https://github.com/SpongePowered/Mixin/wiki) modifies Minecraft classes
at runtime. Samsara uses it to connect modules, input handling and rendering to the game
without distributing modified Minecraft classes.

### Interface and Accounts

[LWJGL NanoVG](https://www.lwjgl.org/customize) renders the custom interface and HUDs.
[MinecraftAuth](https://github.com/CCBlueX/MinecraftAuth) provides account authentication,
and [JavaFX WebView](https://openjfx.io/) supports the embedded Microsoft login window.

## Support

[Support Samsara](https://catfk.com/shop/samsara)
