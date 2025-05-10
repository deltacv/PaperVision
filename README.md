# PaperVision

Create your custom OpenCV algorithms using a user-friendly node editor interface inspired by Blender and Unreal Engine blueprints.

<img src="https://raw.githubusercontent.com/deltacv/EOCV-Sim/refs/heads/main/EOCV-Sim/src/main/resources/images/papervision.gif"/>

# Running 

## Run with EOCV-Sim (recommended)

PaperVision comes bundled with EOCV-Sim, starting from v4.0.0. [You can download the latest version of EOCV-Sim from the docs](https://docs.deltacv.org/eocv-sim/downloading-eocv-sim).<br><br>
Make sure to refer to the "PaperVision" tab in the simulator to create a new project and open it right away ! The integration with EOCV-Sim will allow you to live preview your pipeline as you build it in the node editor.

## Run with Gradle

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
