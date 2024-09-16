package com.particle_life.app.io;

import com.particle_life.Particle;

import java.io.*;

public class ParticlesIO {
    public static Particle[] loadParticles(InputStream in) {
        return new BufferedReader(new InputStreamReader(in))
                .lines()
                .skip(1)  // skip header
                .map(line -> {
                    String[] parts = line.split("\t");
                    Particle particle = new Particle();
                    // scale from [0, 1) to [-1, 1).
                    particle.position.set(
                            2 * Double.parseDouble(parts[0]) - 1,
                            2 * Double.parseDouble(parts[1]) - 1,
                            0
                    );
                    particle.velocity.set(
                            2 * Double.parseDouble(parts[2]),
                            2 * Double.parseDouble(parts[3]),
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
                writer.println("x\ty\tvx\tvy\ttype");
                for (Particle particle : particles) {
                    // normalize from [-1, 1) to [0,1)
                    writer.println((0.5 * particle.position.x + 0.5) + "\t"
                            + (0.5 * particle.position.y + 0.5) + "\t"
                            + (0.5 * particle.velocity.x) + "\t"
                            + (0.5 * particle.velocity.y) + "\t"
                            + particle.type);
                }
                writer.flush();
            }
            out.write(byteStream.toByteArray());
        }
    }
}
