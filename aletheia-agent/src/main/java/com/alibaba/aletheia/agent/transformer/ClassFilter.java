package com.alibaba.aletheia.agent.transformer;

import com.alibaba.aletheia.agent.config.AgentConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 类过滤器
 * 实现白名单/黑名单过滤策略
 *
 * @author Aletheia Team
 */
public class ClassFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFilter.class);

    private final AgentConfig config;

    public ClassFilter(AgentConfig config) {
        this.config = config;
    }

    /**
     * 判断是否应该增强该类
     *
     * @param className 类名（内部格式，如 com/example/Test）
     * @param classfileBuffer 类文件字节码
     * @return true 如果应该增强
     */
    public boolean shouldTransform(String className, byte[] classfileBuffer) {
        if (className == null || classfileBuffer == null) {
            return false;
        }

        // 检查类大小
        if (classfileBuffer.length > config.getMaxClassSize()) {
            LOGGER.debug("Class {} too large ({} bytes), skipping", className, classfileBuffer.length);
            return false;
        }

        // 检查排除模式（黑名单）
        for (String excludePattern : config.getExcludePatterns()) {
            if (className.startsWith(excludePattern)) {
                return false;
            }
        }

        // 如果有包含模式（白名单），则只增强匹配的类
        Set<String> includePatterns = config.getIncludePatterns();
        if (!includePatterns.isEmpty()) {
            boolean matched = false;
            for (String includePattern : includePatterns) {
                if (className.startsWith(includePattern)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        // 检查类类型（不增强接口、抽象类、枚举、注解、Lambda等）
        if (!isTransformableClass(className, classfileBuffer)) {
            return false;
        }

        return true;
    }

    /**
     * 检查类是否可增强
     */
    private boolean isTransformableClass(String className, byte[] classfileBuffer) {
        // Lambda 表达式生成的类
        if (className.contains("$$Lambda$")) {
            return false;
        }

        // 内部类（可选，可以根据配置决定）
        if (className.contains("$") && !config.getIncludePatterns().isEmpty()) {
            // 如果有白名单，内部类也需要匹配
            return true;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            int access = cr.getAccess();

            // 接口
            if ((access & Opcodes.ACC_INTERFACE) != 0) {
                return false;
            }

            // 注解
            if ((access & Opcodes.ACC_ANNOTATION) != 0) {
                return false;
            }

            // 枚举
            if ((access & Opcodes.ACC_ENUM) != 0) {
                return false;
            }

            // 抽象类（可选，可以根据配置决定）
            // if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            //     return false;
            // }

        } catch (Exception e) {
            LOGGER.warn("Failed to parse class: {}", className, e);
            return false;
        }

        return true;
    }
}
