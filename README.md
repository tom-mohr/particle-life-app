# 🦠 Particle Life App

A GUI for the [Particle Life Framework](https://github.com/tom-mohr/particle-life).

- [Download](https://particle-life.com) (.exe for Windows)
- [Documentation](https://particle-life.com/docs)

Join the [Discord server](https://discord.gg/Fd64AhKzMD)!

![Screenshot of the App](./readme_assets/app_demo.png)

## Run This App From Source

You can easily run this app from the source code.

First, check that you have

- [Git](https://git-scm.com/downloads) installed.
- [Java](https://jdk.java.net/19/) installed, with a version of at least 16 and at most 22. (Check your version with `java -version`.)

Then, download the source code:
```bash
git clone https://github.com/tom-mohr/particle-life-app.git
```

To run the program, make sure that you navigate into the folder:
```bash
cd particle-life-app
```

Then start the program:
```bash
./gradlew run
```

After some time, the program should launch and you should see particles on your screen.
Feel free to mess with the code!

## Troubleshooting

If you encounter any problems, ask for help in the [`#tech-support`](https://discord.gg/EVG8XnCn3U) channel on the Discord server.

* `Unsupported class file major version 67` (or similar).<br>
  This happens when your installed Java version is higher than 22.
  Check your current Java version with `java -version`.  You need to uninstall Java and install a version between 16 (including) and 22 (including).
  Make sure that after installing the new Java version, `java -version` actually outputs the installed version.
  Restarting the computer sometimes helps.

## How to make a release

- Confirm that everything is working correctly and check in with others that the current state of the main branch is ready for release.
- Run `./gradlew zipApp` from the project root.
  This generates the zip file `particle-life-app.zip` in `./build/zipApp/`. It includes the Windows executable (`.exe`) along with other files.
- Go to the [Releases](https://github.com/tom-mohr/particle-life-app/releases) section of this GitHub repo and click `Draft a new release`.
- Click `Choose a tag` and type the new version name:
  - Prefix the version name with the letter `v`. Some good tag names might be `v1.0.0` or `v2.3.4`.
  - The version name should comply with [semantic versioning](https://semver.org/). Talk to others if you are unsure about what to choose here.
- Click `Create a new tag`.
- Set the release title to match the tag name.
- Use the description to summarize the changes of all commits since the last release.
- Add the generated `particle-life-app.zip` as an asset to the release.
- Click `Publish release`.
