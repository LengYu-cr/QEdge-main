package com.liquidglass.java.miba;

/** Direct Java 7 port of catalog/utils/ProgressConverter.kt. */
public interface ProgressConverter {
    float convert(float progress);

    /** Exact Kotlin Default: (1 - exp(-abs(p))) * sign(p). */
    ProgressConverter DEFAULT = new ProgressConverter() {
        public float convert(float progress) {
            float sign = progress < 0f ? -1f : (progress > 0f ? 1f : 0f);
            return (1f - (float) Math.exp(-Math.abs(progress))) * sign;
        }
    };
}
