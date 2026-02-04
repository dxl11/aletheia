package com.alibaba.aletheia.agent.transformer;

import com.alibaba.aletheia.agent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transformer 管理器
 * 统一管理所有 Transformer，支持动态添加/移除
 *
 * @author Aletheia Team
 */
public class TransformerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerManager.class);

    private final Instrumentation instrumentation;
    private final AgentConfig config;
    private final Map<String, BaseTransformer> transformers = new ConcurrentHashMap<>();

    public TransformerManager(Instrumentation instrumentation, AgentConfig config) {
        this.instrumentation = instrumentation;
        this.config = config;
    }

    /**
     * 注册 Transformer
     */
    public void registerTransformer(String name, BaseTransformer transformer, boolean canRetransform) {
        if (transformers.containsKey(name)) {
            LOGGER.warn("Transformer {} already registered, removing old one", name);
            removeTransformer(name);
        }

        instrumentation.addTransformer(transformer, canRetransform);
        transformers.put(name, transformer);
        LOGGER.info("Transformer {} registered (canRetransform={})", name, canRetransform);
    }

    /**
     * 移除 Transformer
     */
    public void removeTransformer(String name) {
        BaseTransformer transformer = transformers.remove(name);
        if (transformer != null) {
            instrumentation.removeTransformer(transformer);
            LOGGER.info("Transformer {} removed", name);
        }
    }

    /**
     * 获取 Transformer
     */
    public BaseTransformer getTransformer(String name) {
        return transformers.get(name);
    }

    /**
     * 初始化默认 Transformer
     */
    public void initDefaultTransformers(boolean canRetransform) {
        // RT Transformer
        MethodRtTransformer rtTransformer = new MethodRtTransformer(config);
        registerTransformer("RT", rtTransformer, canRetransform);

        // Lock Transformer（用于锁竞争诊断）
        if (config.isFeatureEnabled("Lock") || config.isFeatureEnabled("Thread")) {
            com.alibaba.aletheia.agent.diagnostic.transformer.LockTransformer lockTransformer =
                    new com.alibaba.aletheia.agent.diagnostic.transformer.LockTransformer(config);
            registerTransformer("Lock", lockTransformer, canRetransform);
        }
    }

    /**
     * 启用 Transformer
     */
    public void enableTransformer(String name) {
        BaseTransformer transformer = transformers.get(name);
        if (transformer != null) {
            LOGGER.info("Transformer {} enabled", name);
        } else {
            LOGGER.warn("Transformer {} not found", name);
        }
    }

    /**
     * 禁用 Transformer
     */
    public void disableTransformer(String name) {
        BaseTransformer transformer = transformers.get(name);
        if (transformer != null) {
            LOGGER.info("Transformer {} disabled", name);
        } else {
            LOGGER.warn("Transformer {} not found", name);
        }
    }
}
