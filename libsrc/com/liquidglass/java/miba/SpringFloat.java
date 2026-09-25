package com.liquidglass.java.miba;

/**
 * Float spring used by the Java port. The step is the closed-form damped
 * harmonic oscillator, matching the spring model used by Compose much more
 * closely than a frame-rate-dependent Euler integrator.
 */
final class SpringFloat {
    float value;
    float velocity;
    float target;
    float dampingRatio;
    float stiffness;
    float threshold;

    SpringFloat(float value, float dampingRatio, float stiffness, float threshold) {
        this.value = value;
        this.target = value;
        this.dampingRatio = dampingRatio;
        this.stiffness = stiffness;
        this.threshold = Math.max(0.000001f, threshold);
    }

    void snapTo(float v) {
        value = v;
        target = v;
        velocity = 0f;
    }

    void animateTo(float v) {
        target = v;
    }

    boolean step(float dt) {
        if (dt <= 0f) return false;
        // Animations can resume after a long pause; do not integrate a huge jump.
        if (dt > 0.064f) dt = 0.064f;

        double omega0 = Math.sqrt(Math.max(0.000001f, stiffness));
        double zeta = dampingRatio;
        double y0 = value - target;
        double v0 = velocity;
        double y;
        double v;

        if (zeta < 1.0 - 1.0e-4) {
            double omegaD = omega0 * Math.sqrt(1.0 - zeta * zeta);
            double envelope = Math.exp(-zeta * omega0 * dt);
            double cos = Math.cos(omegaD * dt);
            double sin = Math.sin(omegaD * dt);
            double b = (v0 + zeta * omega0 * y0) / omegaD;
            double inner = y0 * cos + b * sin;
            y = envelope * inner;
            v = envelope * (
                    -zeta * omega0 * inner
                            + (-y0 * omegaD * sin + b * omegaD * cos)
            );
        } else if (zeta > 1.0 + 1.0e-4) {
            double s = Math.sqrt(zeta * zeta - 1.0);
            double r1 = -omega0 * (zeta - s);
            double r2 = -omega0 * (zeta + s);
            double c1 = (v0 - r2 * y0) / (r1 - r2);
            double c2 = y0 - c1;
            double e1 = Math.exp(r1 * dt);
            double e2 = Math.exp(r2 * dt);
            y = c1 * e1 + c2 * e2;
            v = r1 * c1 * e1 + r2 * c2 * e2;
        } else {
            double envelope = Math.exp(-omega0 * dt);
            double c = v0 + omega0 * y0;
            y = envelope * (y0 + c * dt);
            v = envelope * (v0 - omega0 * c * dt);
        }

        value = target + (float) y;
        velocity = (float) v;

        // Compose stops once both displacement and velocity are visually below
        // the visibility threshold. The velocity multiplier keeps the value from
        // snapping while it is still visibly moving.
        if (Math.abs(value - target) <= threshold
                && Math.abs(velocity) <= threshold * 62.5f) {
            value = target;
            velocity = 0f;
        }
        return value != target || velocity != 0f;
    }
}
