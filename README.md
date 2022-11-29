# 🦠 Particle Life App

A GUI for the [Particle Life Framework](https://github.com/tom-mohr/particle-life).

- [Download](https://particle-life.com)
- [Documentation](https://particle-life.com/app/docs)

Join the [Discord server](https://discord.gg/tgJpgru9)!

![Screenshot of the App](./readme_assets/app_demo.png)

## How to Build

If you just want to try out this application, simply use the download link above.
However, you can also build this project yourself, e.g. if you want to modify the code or even contribute to this repository.
For that, you need Java 16 or higher.

- To start the application, run `./gradlew run` from the project root.
- To generate an executable, run `./gradlew assembleApp` from the project root.
This generates an `.exe` along with other files in `./build/app/`.
- Before a release, run `./gradlew zipApp` from the project root.
This generates a zip file in `./build/zipApp/` that must be added as an asset to the release.
