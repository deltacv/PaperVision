# PaperVision

Create your custom OpenCV algorithms using a user-friendly node editor interface inspired by Blender and Unreal Engine blueprints.

https://github.com/user-attachments/assets/e2cfc9b6-d390-4b8c-b165-b36dcb664b62

# Beta Testing the EOCV-Sim plugin

Thank you for your interesting in beta-testing PaperVision. I'm really happy to finally have functional preview builds after stalling on this project numerous times through the last three years,
Although PaperVision is able to run the user interface on its own, it was made with EOCV-Sim integration in mind since day one as the backend engine, which pretty much allows to have a live visualization of the pipeline as you mess around in the node editor. Use the following instructions to get it all up and running.

1. [Download and open EOCV-Sim 3.8.1+](https://docs.deltacv.org/eocv-sim/downloading-eocv-sim).<br><br>
2. Go to `File -> Manage Plugins` and click on "Open Plugins Folder". You'll find a `repository.toml` file in that folder, open it with your favorite text editor.<br><br>
3. Add the following line under the `[plugins]` section:
   - `PaperVision = "com.github.deltacv.PaperVision:EOCVSimPlugin:master-SNAPSHOT"`<br><br>
4. Your `repository.toml` file should look like this:
```toml
[repositories]
# Declare the URL of the Maven repositories to use, with a friendly name.
# The URL must preferably end with a slash to be handled correctly.
central = "https://repo.maven.apache.org/maven2/"
jitpack = "https://jitpack.io/"

[plugins]
# Declare the plugin ID and the Maven coordinates of the plugin, similar to how you do it in Gradle.
# (group:artifact:version) which will be used to download the plugin from one of Maven repositories.
# Any dependency that does not have a plugin.toml in its jar will not be considered after download.
PaperVision = "com.github.deltacv.PaperVision:EOCVSimPlugin:master-SNAPSHOT"
```
5. Restart EOCV-Sim after saving the file, and make sure accept the SuperAccess upon prompting (PaperVision won't work without SuperAccess as it needs it to load LWJGL onto your system, among other filesystem interactions)<br><br>
   - **Please note that EOCV-Sim will complain about the unverified origin of the plugin.** As long as you are copying the PaperVision plugin entry correctly and not adding additional entries under the `[repositories]` section, you can safely ignore those warnings.
   - As a beta-tester, it is recommended to run EOCV-Sim on a terminal with `java -jar /path/to/EOCV-Sim-<version>-all.jar` to catch any issues and report them to GitHub issues or the discord server<br><br>
6. Locate the PaperVision tab in the simulator's top-right, create a new project using the provided interface, and open it right away.<br><br>
7. Happy testing!<br><br>

# Run with Gradle

Use the following commands to run the project with gradle, this will allow you to test the latest features and changes, building from source.
<br>

```shell
git clone https://github.com/deltacv/PaperVision.git
cd PaperVision
./gradlew runEv
```

<br>This option won't allow for live previewing, since the backend runs in NO-OP mode, but it will let you to test the node editor.

# Community

Please [join the discord server](https://discord.gg/A3RMYzf6DA) to keep an active beta testing community, where you'll be able to report issues and suggest new features. Please note that off-topic discourse is not allowed on the server, and in the case of underage FIRST participants, YPP policies will be actively enforced.
