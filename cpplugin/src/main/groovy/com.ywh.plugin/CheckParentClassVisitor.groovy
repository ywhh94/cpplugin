package com.ywh.plugin;

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class CheckParentClassVisitor extends ClassVisitor {
    private String className;
    private String superName;

    public CheckParentClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);

    }

    /**
     *
     * @param version
     * @param access
     * @param name             class名
     * @param signature
     * @param superName        父类名字
     * @param interfaces       实现的接口数组
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.superName = superName;
    }

    /**
     *
     * @param access
     * @param name           函数名
     * @param descriptor
     * @param signature
     * @param exceptions
     * @return
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("------------------------ClassVisitor------------------------");
        System.out.println("visitMethod -- name:" + name + ", superName:" + superName + ", className:" + className + ", access:" + access);
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (superName.equals("androidx/appcompat/app/AppCompatActivity")) {
            if (name.startsWith("onCreate")) {
                return new CheckParentMethodVisitor(mv, className, name);
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        super.visitEnd()
    }
}
