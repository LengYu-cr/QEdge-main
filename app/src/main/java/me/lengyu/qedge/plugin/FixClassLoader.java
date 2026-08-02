package me.lengyu.qedge.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class FixClassLoader extends ClassLoader {
    private final List<ClassLoader> classLoaders = new ArrayList<>();
    private ClassLoader hostClassLoader;

    public FixClassLoader() {
        try {
            hostClassLoader = (ClassLoader) me.lengyu.qedge.utils.ClassUtils.class
                    .getDeclaredField("hostClassLoader").get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addClassLoader(ClassLoader loader) {
        if (loader != null) {
            classLoaders.add(loader);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : classLoaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (hostClassLoader != null) {
            try {
                return hostClassLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String name) {
        for (ClassLoader loader : classLoaders) {
            URL resource = loader.getResource(name);
            if (resource != null) {
                return resource;
            }
        }
        if (hostClassLoader != null) {
            return hostClassLoader.getResource(name);
        }
        return null;
    }
}