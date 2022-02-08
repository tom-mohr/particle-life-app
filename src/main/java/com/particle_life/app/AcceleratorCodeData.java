package com.particle_life.app;

import com.particle_life.Accelerator;
import org.joml.Vector3d;

public class AcceleratorCodeData {

    public boolean mayEdit;
    public String importCode;
    public String className;

    /**
     * Body of the method {@link Accelerator#accelerate(double a, Vector3d x)}.
     */
    public String methodCode;

    /**
     * The compiled accelerator.
     */
    public Accelerator accelerator;

    public AcceleratorCodeData(boolean mayEdit, String importCode, String className, String methodCode, Accelerator accelerator) {
        this.mayEdit = mayEdit;
        this.importCode = importCode;
        this.className = className;
        this.methodCode = methodCode;
        this.accelerator = accelerator;
    }
}
