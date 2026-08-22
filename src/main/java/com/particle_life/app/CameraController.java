package com.particle_life.app;

import com.particle_life.app.utils.CamOperations;
import com.particle_life.app.utils.MathUtils;
import com.particle_life.backend.Clock;
import com.particle_life.backend.PhysicsSettings;
import org.joml.Vector2d;

class CameraController {

    final Vector2d camPos = new Vector2d(0.5, 0.5);
    final Vector2d camPosGoal = new Vector2d(camPos);
    double camSize = 1.0;
    double camSizeGoal = 1.0;

    void update(
            AppSettings appSettings,
            Clock renderClock,
            InputState input,
            int width,
            int height,
            double pmouseX,
            double pmouseY,
            double mouseX,
            double mouseY
    ) {
        if (input.draggingShift) {
            new CamOperations(camPos, camSize, width, height)
                    .dragCam(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY));
            camPosGoal.set(camPos);
        }

        double camMovementStepSize = appSettings.camMovementSpeed * camSize;
        camMovementStepSize *= renderClock.getDtMillis() / 1000.0;
        if (input.leftPressed || input.aPressed) camPosGoal.add(-camMovementStepSize, 0.0);
        if (input.rightPressed || input.dPressed) camPosGoal.add(camMovementStepSize, 0.0);
        if (input.upPressed || input.wPressed) camPosGoal.add(0.0, -camMovementStepSize);
        if (input.downPressed || input.sPressed) camPosGoal.add(0.0, camMovementStepSize);

        camPos.lerp(camPosGoal, appSettings.shiftSmoothness);
        camSize = MathUtils.lerp(camSize, camSizeGoal, appSettings.zoomSmoothness);
    }

    void reset(PhysicsSettings settings, int width, int height, boolean fit) {
        if (settings.wrap) camPos.sub(Math.floor(camPos.x), Math.floor(camPos.y));
        camPosGoal.set(0.5, 0.5);
        camSizeGoal = 1;

        if (fit) {
            camSizeGoal = (double) Math.min(width, height) / Math.max(width, height);
        }
    }

    void scrollZoom(AppSettings appSettings, double mouseX, double mouseY, int width, int height, double scrollY, double maxCamSize) {
        double factor = Math.pow(appSettings.zoomStepFactor, -scrollY);
        CamOperations cam = new CamOperations(camPosGoal, camSizeGoal, width, height);
        cam.zoom(mouseX, mouseY, Math.min(camSizeGoal * factor, maxCamSize));
        camSizeGoal = cam.camSize;
    }
}
