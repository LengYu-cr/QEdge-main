package com.liquidglass.java.miba;

/**
 * Java counterpart of com.kyant.backdrop.RuntimeShader.
 * The framework android.graphics.RuntimeShader instance is kept behind Object
 * so this class can be loaded safely on Android versions below API 33.
 */
public final class RuntimeShader {
    private final Object shader;

    public RuntimeShader(String shaderString) {
        if (!Platform.isRuntimeShaderSupported()) {
            throw new UnsupportedOperationException("RuntimeShader requires Android 13 / API 33+");
        }
        shader = Api33.create(shaderString);
    }

    public void setFloatUniform(String name, float value) { Api33.setFloat(shader, name, new float[] { value }); }
    public void setFloatUniform(String name, float value1, float value2) { Api33.setFloat(shader, name, new float[] { value1, value2 }); }
    public void setFloatUniform(String name, float value1, float value2, float value3) { Api33.setFloat(shader, name, new float[] { value1, value2, value3 }); }
    public void setFloatUniform(String name, float value1, float value2, float value3, float value4) { Api33.setFloat(shader, name, new float[] { value1, value2, value3, value4 }); }
    public void setFloatUniform(String name, float[] values) { Api33.setFloat(shader, name, values); }

    public void setIntUniform(String name, int value) { Api33.setInt(shader, name, new int[] { value }); }
    public void setIntUniform(String name, int value1, int value2) { Api33.setInt(shader, name, new int[] { value1, value2 }); }
    public void setIntUniform(String name, int value1, int value2, int value3) { Api33.setInt(shader, name, new int[] { value1, value2, value3 }); }
    public void setIntUniform(String name, int value1, int value2, int value3, int value4) { Api33.setInt(shader, name, new int[] { value1, value2, value3, value4 }); }
    public void setIntUniform(String name, int[] values) { Api33.setInt(shader, name, values); }

    public void setColorUniform(String name, int argb) { Api33.setColor(shader, name, argb); }

    /** Android RuntimeShader uniform-shader input. API 33+. */
    public void setInputShader(String name, Object inputShader) { Api33.setInputShader(shader, name, inputShader); }

    /** Android RuntimeShader raw BitmapShader buffer input. API 33+. */
    public void setInputBuffer(String name, Object bitmapShader) { Api33.setInputBuffer(shader, name, bitmapShader); }

    /** Advanced/Lua interop escape hatch; returns android.graphics.RuntimeShader on API 33+. */
    public Object getFrameworkShader() { return shader; }

    Object frameworkShader() { return shader; }

    private static final class Api33 {
        private Api33() {}

        static Object create(String source) { return new android.graphics.RuntimeShader(source); }

        static void setFloat(Object object, String name, float[] values) {
            android.graphics.RuntimeShader shader = (android.graphics.RuntimeShader) object;
            if (values == null) return;
            switch (values.length) {
                case 1: shader.setFloatUniform(name, values[0]); break;
                case 2: shader.setFloatUniform(name, values[0], values[1]); break;
                case 3: shader.setFloatUniform(name, values[0], values[1], values[2]); break;
                case 4: shader.setFloatUniform(name, values[0], values[1], values[2], values[3]); break;
                default: shader.setFloatUniform(name, values); break;
            }
        }

        static void setInt(Object object, String name, int[] values) {
            android.graphics.RuntimeShader shader = (android.graphics.RuntimeShader) object;
            if (values == null) return;
            switch (values.length) {
                case 1: shader.setIntUniform(name, values[0]); break;
                case 2: shader.setIntUniform(name, values[0], values[1]); break;
                case 3: shader.setIntUniform(name, values[0], values[1], values[2]); break;
                case 4: shader.setIntUniform(name, values[0], values[1], values[2], values[3]); break;
                default: shader.setIntUniform(name, values); break;
            }
        }

        static void setColor(Object object, String name, int color) {
            ((android.graphics.RuntimeShader) object).setColorUniform(name, color);
        }

        static void setInputShader(Object object, String name, Object inputShader) {
            if (!(inputShader instanceof android.graphics.Shader)) throw new IllegalArgumentException("inputShader must be android.graphics.Shader");
            ((android.graphics.RuntimeShader) object).setInputShader(name, (android.graphics.Shader) inputShader);
        }

        static void setInputBuffer(Object object, String name, Object inputShader) {
            if (!(inputShader instanceof android.graphics.BitmapShader)) throw new IllegalArgumentException("inputShader must be android.graphics.BitmapShader");
            ((android.graphics.RuntimeShader) object).setInputBuffer(name, (android.graphics.BitmapShader) inputShader);
        }
    }
}
