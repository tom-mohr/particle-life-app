#!/bin/bash

echo "Building Particle Life for macOS..."

# Clean and build
./gradlew clean fatJar

# Create app using jpackage
jpackage \
  --type app-image \
  --name ParticleLife \
  --input build/libs \
  --main-jar particle-life-app-all.jar \
  --dest build/dist \
  --verbose

echo "Done! App is at build/dist/ParticleLife.app"
