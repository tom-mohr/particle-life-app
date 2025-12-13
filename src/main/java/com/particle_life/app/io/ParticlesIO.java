package com.particle_life.app.io;

import com.particle_life.backend.Particle;

import java.io.*;

public class ParticlesIO {
    public static Particle[] loadParticles(InputStream in) {
        return new BufferedReader(new InputStreamReader(in))
                .lines()
                .skip(1)  // skip header
                .map(line -> {
                    String[] parts = line.split("\t");
                    Particle particle = new Particle();
                    particle.position.set(
                            Double.parseDouble(parts[0]),
                            Double.parseDouble(parts[1]),
                            0
                    );
                    particle.velocity.set(
                            Double.parseDouble(parts[2]),
                            Double.parseDouble(parts[3]),
                            0
                    );
                    particle.type = Integer.parseInt(parts[4]);
                    return particle;
                })
                .toArray(Particle[]::new);
    }

    public static void saveParticles(Particle[] particles, OutputStream out) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (PrintWriter writer = new PrintWriter(byteStream)) {
                writer.println("x\ty\tvx\tvy\tcolor");
                for (Particle particle : particles) {
                    writer.println(particle.position.x + "\t"
                            + particle.position.y + "\t"
                            + particle.velocity.x + "\t"
                            + particle.velocity.y + "\t"
                            + particle.type);
                }
                writer.flush();
            }
            out.write(byteStream.toByteArray());
        }
    }
}
