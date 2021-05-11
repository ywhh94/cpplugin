package com.ywh.plugin.checkbase;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CheckBaseClassVisitor extends ClassVisitor {

    private String className;

    public CheckBaseClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
        Utils.putActivityWithParent(name, superName);
    }

    //可以 根据name == onCreate && descriptor == (Landroid/os/Bundle;)V判断是Activity
    //但是如果有class自己写了一个onCreate(Bundler bundle)的函数就会误判是Activity
    //还是暂时无法准确地判断是否是Activity
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
//        System.out.println("visitMethod:" + className + ",method:" + name + ",descriptor:" + descriptor);

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    //执行优先于visitMethod
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
//        System.out.println("descriptor:" + descriptor);
        Utils.addBaseClass(className, descriptor);
        return super.visitAnnotation(descriptor, visible);
    }
}
