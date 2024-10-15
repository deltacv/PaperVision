# PaperVision

Create your custom OpenCV algorithms using a user-friendly node editor interface inspired by Blender and Unreal Engine blueprints.

https://github.com/user-attachments/assets/e2cfc9b6-d390-4b8c-b165-b36dcb664b62

# Beta Testing the EOCV-Sim plugin

Thank you for your interesting in beta-testing PaperVision. I'm really happy to finally have functional preview builds after stalling on this project numerous times through the last three years,
Although PaperVision is able to run the user interface on its own, it was made with EOCV-Sim integration in mind since day one as the backend engine, which pretty much allows to have a live visualization of the pipeline as you mess around in the node editor.

EOCV-Sim recently implemented its own plugin system inspired by Minecraft Bukkit, so adding PaperVision to the latest releases of the simulator turns out to be fairly simple as it will be explained next;

1. Download and run [EOCV-Sim 3.7.0+](https://deltacv.gitbook.io/eocv-sim/downloading-eocv-sim). Close EOCV-Sim after running it for the first time.
2. Download the PaperVision plugin jar file from the [development release page](https://github.com/deltacv/PaperVision/releases/tag/Dev)
3. Locate the .eocvsim folder in your filesystem:
   - On Windows: `C:\Users\<your user>\.eocvsim`
   - On MacOS: `/Users/<your user>/.eocvsim`
   - On Linux: `/home/<your user>/.eocvsim`
   - In the case of MacOS/Linux, you will have to enable some "Show Hidden Files" option in your file explorer.
4. Locate the `plugins` folder inside `.eocvsim` and copy the PaperVision plugin jar into it
5. Run EOCV-Sim and accept the SuperAccess prompt (PaperVision won't work without SuperAccess as it needs it to load LWJGL onto your system, among other filesystem interactions)
   - As a beta-tester, it is recommended to run EOCV-Sim on a terminal with `java -jar` to catch any issues and report them to GitHub issues or the discord server
6. Locate the PaperVision tab in the simulator's top-right, create a new project using the provided interface, and open it right away.
7. Happy testing!

Please [join the discord server](https://discord.gg/A3RMYzf6DA) to keep an active beta testing community, where you'll be able to report issues and suggest new features. Please note that off-topic discourse is not allowed on the server, and in the case of underage FIRST participants, YPP policies will be actively enforced.
