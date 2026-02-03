# PaperVision

Create your custom OpenCV algorithms using a user-friendly node editor interface inspired by Blender and Unreal Engine blueprints.

<img src="https://raw.githubusercontent.com/deltacv/EOCV-Sim/refs/heads/main/EOCV-Sim/src/main/resources/images/papervision.gif"/>

# Getting Started

## Run with EOCV-Sim (recommended)

PaperVision is available out of the box in EOCV-Sim v4.0.0 and later. [You can download the latest version of EOCV-Sim from the docs here](https://docs.deltacv.org/eocv-sim/downloading-eocv-sim).<br><br>
**Create a new project from the PaperVision tab in the simulator and start experimenting right away.**
EOCV-Sim integration enables live pipeline previews directly from the node editor.

## Run with Gradle (development)

Use the following commands to run the project with gradle, this will allow you to test the latest features and changes, building from source.
<br>

```shell
git clone https://github.com/deltacv/PaperVision.git
cd PaperVision
./gradlew runEv
```

<br>**Live previewing is unavailable in this mode** (the backend runs in NO-OP due to the absence of EOCV-Sim), but the node editor remains fully testable.

# Community

Please [join the discord server](https://discord.gg/A3RMYzf6DA) to keep an active beta testing community, where you'll be able to report issues and suggest new features. Please note that off-topic discourse is not allowed on the server, and in the case of underage FIRST participants, YPP policies will be actively enforced.
