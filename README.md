# 🦠 Particle Life App

A GUI for [github.com/tom-mohr/particle-life](https://github.com/tom-mohr/particle-life).

# Requirements

- Windows
- Java 16 or higher

# Build, Execution

Open the project in an IDE and run the class `com.particle_life.app.Main`.

If you don't want to use an IDE or want to generate an executable JAR file, run the following command from the project root:
```sh
./gradlew shadowJar
```
This generates a JAR file in `./build/libs/` than can be run with `java -jar <filename>`.


# Shortcuts

| Keys       | Action                       |
|------------|------------------------------|
| `l` / `L`  | change palette               |
| `s` / `S`  | change shader                |
| `v` / `V`  | change accelerator           |
| `x` / `X`  | change position setter       |
| `r` / `R`  | change matrix generator      |
| `p`        | set positions                |
| `t`        | set types                    |
| `m`        | set matrix                   |
| `w`        | toggle space wrapping        |
| `SPACE`    | pause physics                |
| `F11`      | toggle full screen           |
| `ALT`+`F4` | quit                         |
| `+` / `-`  | zoom                         |
| `z`        | reset zoom                   |
| `Z`        | reset zoom (fit window)      |
| `a`        | toggle advanced GUI          |
| `c`        | toggle traces (clear screen) |
| `h`        | hide GUI / show GUI          |
