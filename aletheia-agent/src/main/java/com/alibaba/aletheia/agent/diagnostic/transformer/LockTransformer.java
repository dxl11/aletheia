package com.alibaba.aletheia.agent.diagnostic.transformer;

import com.alibaba.aletheia.agent.config.AgentConfig;
import com.alibaba.aletheia.agent.transformer.BaseTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * 锁增强 Transformer
 * 在 synchronized 方法和 Lock.lock() 调用处添加锁竞争监控
 *
 * @author Aletheia Team
 */
public class LockTransformer extends BaseTransformer {

    private static final String DIAGNOSTIC_HELPER_CLASS = "com/alibaba/aletheia/agent/diagnostic/DiagnosticHelper";

    public LockTransformer(AgentConfig config) {
        super(config);
    }

    @Override
    protected boolean isFeatureEnabled() {
        return config.isFeatureEnabled("Lock") || config.isFeatureEnabled("Thread");
    }

    @Override
    protected byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            ClassReader classReader = new ClassReader(classfileBuffer);
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
            LockClassAdapter adapter = new LockClassAdapter(classWriter, className);

            classReader.accept(adapter, ClassReader.EXPAND_FRAMES);
            return classWriter.toByteArray();
        } catch (Exception e) {
            logger.warn("Failed to transform class for lock monitoring: {}", className, e);
            return null;
        }
    }

    /**
     * 类适配器
     */
    private static class LockClassAdapter extends org.objectweb.asm.ClassVisitor {

        private String className;

        LockClassAdapter(org.objectweb.asm.ClassVisitor cv, String className) {
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

            // 排除构造函数和静态初始化块
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return mv;
            }

            // 检查是否是 synchronized 方法
            boolean isSynchronized = (access & Opcodes.ACC_SYNCHRONIZED) != 0;

            if (isSynchronized) {
                return new SynchronizedMethodAdapter(mv, className, name, descriptor);
            }

            return new LockMethodAdapter(mv, className, name, descriptor);
        }
    }

    /**
     * synchronized 方法适配器
     */
    private static class SynchronizedMethodAdapter extends org.objectweb.asm.MethodVisitor {

        private String className;
        private String methodName;

        SynchronizedMethodAdapter(org.objectweb.asm.MethodVisitor mv, String className,
                                  String methodName, String descriptor) {
            super(Opcodes.ASM9, mv);
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public void visitCode() {
            super.visitCode();

            // 生成锁标识
            String lockIdentity = className.replace('/', '.') + "." + methodName + " (synchronized)";

            // 在方法开始处注入锁获取监控
            mv.visitLdcInsn(lockIdentity);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/lang/Thread", "getId", "()J", false);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    DIAGNOSTIC_HELPER_CLASS,
                    "recordLockAcquire",
                    "(Ljava/lang/String;J)V",
                    false);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN || opcode == Opcodes.IRETURN
                    || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN
                    || opcode == Opcodes.DRETURN || opcode == Opcodes.ARETURN
                    || opcode == Opcodes.ATHROW) {
                // 在方法返回前注入锁释放监控
                String lockIdentity = className.replace('/', '.') + "." + methodName + " (synchronized)";
                mv.visitLdcInsn(lockIdentity);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Thread", "getId", "()J", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        DIAGNOSTIC_HELPER_CLASS,
                        "recordLockRelease",
                        "(Ljava/lang/String;J)V",
                        false);
            }
            super.visitInsn(opcode);
        }
    }

    /**
     * 普通方法适配器（用于监控 Lock.lock() 调用）
     */
    private static class LockMethodAdapter extends org.objectweb.asm.MethodVisitor {

        LockMethodAdapter(org.objectweb.asm.MethodVisitor mv, String className,
                         String methodName, String descriptor) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            // 监控 Lock.lock() 调用
            if (opcode == Opcodes.INVOKEINTERFACE && "java/util/concurrent/locks/Lock".equals(owner)
                    && "lock".equals(name) && "()V".equals(descriptor)) {
                // 在 lock() 调用前记录
                mv.visitInsn(Opcodes.DUP); // 复制 Lock 对象引用
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Thread", "getId", "()J", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        DIAGNOSTIC_HELPER_CLASS,
                        "recordLockAcquire",
                        "(Ljava/lang/String;J)V",
                        false);
            }

            // 监控 Lock.unlock() 调用
            if (opcode == Opcodes.INVOKEINTERFACE && "java/util/concurrent/locks/Lock".equals(owner)
                    && "unlock".equals(name) && "()V".equals(descriptor)) {
                // 在 unlock() 调用前记录
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Thread", "getId", "()J", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                        DIAGNOSTIC_HELPER_CLASS,
                        "recordLockRelease",
                        "(Ljava/lang/String;J)V",
                        false);
            }

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
