package com.liquidglass.java.miba;

import java.util.HashMap;
import java.util.Map;

/** Direct Java counterpart of RuntimeShaderCache.kt. */
public final class RuntimeShaderCache {
    private final Map<String, RuntimeShader> runtimeShaders = new HashMap<String, RuntimeShader>();

    public RuntimeShader obtainRuntimeShader(String key, String shaderString) {
        RuntimeShader shader = runtimeShaders.get(key);
        if (shader == null) {
            shader = new RuntimeShader(shaderString);
            runtimeShaders.put(key, shader);
        }
        return shader;
    }

    public RuntimeShader get(String key) { return runtimeShaders.get(key); }
    public boolean contains(String key) { return runtimeShaders.containsKey(key); }
    public int size() { return runtimeShaders.size(); }
    public RuntimeShader remove(String key) { return runtimeShaders.remove(key); }
    public void clear() { runtimeShaders.clear(); }
}
