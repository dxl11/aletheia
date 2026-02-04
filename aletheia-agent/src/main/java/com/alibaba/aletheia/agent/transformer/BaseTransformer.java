package com.alibaba.aletheia.agent.transformer;

import com.alibaba.aletheia.agent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * 基础 Transformer
 * 提供统一的 transform 接口和异常处理
 *
 * @author Aletheia Team
 */
public abstract class BaseTransformer implements ClassFileTransformer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ClassFilter classFilter;
    protected final AgentConfig config;

    public BaseTransformer(AgentConfig config) {
        this.config = config;
        this.classFilter = new ClassFilter(config);
    }

    @Override
    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                 ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            // 快速过滤：检查是否应该增强
            if (!classFilter.shouldTransform(className, classfileBuffer)) {
                return null;
            }

            // 检查功能是否启用
            if (!isFeatureEnabled()) {
                return null;
            }

            // 执行实际的转换逻辑
            return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

        } catch (Throwable e) {
            // 捕获所有异常，避免影响类加载
            logger.warn("Failed to transform class: {}", className, e);
            return null;
        }
    }

    /**
     * 执行实际的转换逻辑
     * 子类实现此方法
     */
    protected abstract byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException;

    /**
     * 检查功能是否启用
     * 子类可以重写此方法以检查特定的功能开关
     */
    protected boolean isFeatureEnabled() {
        return true; // 默认启用
    }
}
