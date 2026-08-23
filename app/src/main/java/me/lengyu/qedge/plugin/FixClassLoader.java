package me.lengyu.qedge.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import me.lengyu.qedge.utils.LogUtils;
/**
 * @Author 冷雨
 * @Description ClassLoader 修复类
 * 用于修复 ClassLoader 问题，确保模块自身的类可被加载
 */
public class FixClassLoader extends ClassLoader {
    private final List<ClassLoader> classLoaders = new ArrayList<>();
    private final ClassLoader hostClassLoader;

    public FixClassLoader() {
        hostClassLoader = me.lengyu.qedge.utils.ReflectUtils.hostClassLoader;
        // 添加模块自身的 ClassLoader，确保 BeanShell 等模块类可被加载
        classLoaders.add(getClass().getClassLoader());
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