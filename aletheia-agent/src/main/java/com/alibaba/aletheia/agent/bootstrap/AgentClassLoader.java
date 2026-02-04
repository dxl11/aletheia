package com.alibaba.aletheia.agent.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Agent 专用 ClassLoader
 * 用于隔离 Agent 类，避免 ClassLoader 泄漏
 *
 * @author Aletheia Team
 */
public class AgentClassLoader extends URLClassLoader {

    static {
        // 注册为并行类加载器（Java 7+）
        registerAsParallelCapable();
    }

    public AgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public AgentClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // 优先从父类加载器加载（避免重复加载系统类）
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    // 先尝试从父类加载器加载
                    if (getParent() != null) {
                        c = getParent().loadClass(name);
                    }
                } catch (ClassNotFoundException e) {
                    // 父类加载器找不到，从当前 ClassLoader 加载
                }
                if (c == null) {
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
