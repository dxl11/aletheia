package com.alibaba.aletheia.agent.transformer;

import com.alibaba.aletheia.agent.sampler.RtSampler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * 方法字节码转换器
 * 用于在方法入口和出口添加 RT 统计埋点
 *
 * @author Aletheia Team
 */
public class MethodTransformer implements ClassFileTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodTransformer.class);

    /**
     * 需要排除的包名前缀
     */
    private static final String[] EXCLUDED_PACKAGES = {
            "com/alibaba/aletheia",
            "java/",
            "javax/",
            "sun/",
            "com/sun/",
            "org/slf4j",
            "ch/qos/logback",
            "org/objectweb/asm"
    };

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            // 排除系统类和 Aletheia 自身类
            if (shouldExclude(className)) {
                return null;
            }

            // 使用 ASM 进行字节码增强
            ClassReader classReader = new ClassReader(classfileBuffer);
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            MethodAdapter methodAdapter = new MethodAdapter(classWriter, className);

            classReader.accept(methodAdapter, ClassReader.EXPAND_FRAMES);
            return classWriter.toByteArray();
        } catch (Exception e) {
            LOGGER.warn("Failed to transform class: {}", className, e);
            return null;
        }
    }

    /**
     * 判断是否应该排除该类
     *
     * @param className 类名
     * @return true 如果应该排除
     */
    private boolean shouldExclude(String className) {
        if (className == null) {
            return true;
        }
        for (String excludedPackage : EXCLUDED_PACKAGES) {
            if (className.startsWith(excludedPackage)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 方法适配器，用于在方法中添加埋点代码
     */
    private static class MethodAdapter extends org.objectweb.asm.ClassVisitor {

        private String className;

        MethodAdapter(org.objectweb.asm.ClassVisitor cv, String className) {
            super(Opcodes.ASM9, cv);
            this.className = className;
        }

        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor,
                                                            String signature, String[] exceptions) {
            org.objectweb.asm.MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
            if (mv == null) {
                return null;
            }

            // 排除构造函数、静态初始化块和抽象方法
            if (name.equals("<init>") || name.equals("<clinit>")
                    || (access & Opcodes.ACC_ABSTRACT) != 0) {
                return mv;
            }

            // 添加方法埋点
            return new MethodVisitorAdapter(mv, className, name, descriptor);
        }
    }

    /**
     * 方法访问器适配器，在方法入口和出口添加统计代码
     */
    private static class MethodVisitorAdapter extends org.objectweb.asm.MethodVisitor {

        private String className;
        private String methodName;
        private String descriptor;

        MethodVisitorAdapter(org.objectweb.asm.MethodVisitor mv, String className,
                             String methodName, String descriptor) {
            super(Opcodes.ASM9, mv);
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            // 在方法入口添加开始时间记录
            String methodSignature = className.replace('/', '.') + "." + methodName;
            injectStartTime(methodSignature);
        }

        @Override
        public void visitInsn(int opcode) {
            // 在方法返回前添加结束时间记录
            if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN
                    || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                    || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN
                    || opcode == Opcodes.ATHROW) {
                String methodSignature = className.replace('/', '.') + "." + methodName;
                injectEndTime(methodSignature);
            }
            super.visitInsn(opcode);
        }

        /**
         * 注入开始时间记录代码
         */
        private void injectStartTime(String methodSignature) {
            // 调用 RtSampler.onMethodStart(methodSignature)
            mv.visitLdcInsn(methodSignature);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/alibaba/aletheia/agent/sampler/RtSampler",
                    "onMethodStart",
                    "(Ljava/lang/String;)V",
                    false);
        }

        /**
         * 注入结束时间记录代码
         */
        private void injectEndTime(String methodSignature) {
            // 调用 RtSampler.onMethodEnd(methodSignature)
            mv.visitLdcInsn(methodSignature);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/alibaba/aletheia/agent/sampler/RtSampler",
                    "onMethodEnd",
                    "(Ljava/lang/String;)V",
                    false);
        }
    }
}
