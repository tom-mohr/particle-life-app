package com.particle_life.app;

import com.particle_life.Accelerator;
import org.joor.Reflect;
import org.joor.ReflectException;

public class AcceleratorCompiler {

    private String errorMessage = null;

    public Accelerator compile(AcceleratorCodeData data) {
        return compile(data.importCode, data.className, data.methodCode);
    }

    public Accelerator compile(String imports, String className, String methodCode) throws ReflectException {
        String fullSourceCode = ("""
                        package com.particle_life.app.accelerators;
                                                
                        %s
                        import com.particle_life.Accelerator;
                        import org.joml.Vector3d;
                        
                        public class %s implements Accelerator {
                            public Vector3d accelerate(double a, Vector3d x) {
                                %s
                            }
                        }
                        """
        ).formatted(imports, className, methodCode);
        String fullClassName = "com.particle_life.app.accelerators." + className;

        try {
            return Reflect.compile(fullClassName, fullSourceCode).create().get();
        } catch (ReflectException e) {
            errorMessage = e.getLocalizedMessage();
            return null;
        }
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public void clearError() {
        errorMessage = null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
