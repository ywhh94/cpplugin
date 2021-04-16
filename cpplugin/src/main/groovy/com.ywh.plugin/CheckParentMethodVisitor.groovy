package com.ywh.plugin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class CheckParentMethodVisitor extends MethodVisitor {
    private String className;
    private String methodName;

    public CheckParentMethodVisitor(MethodVisitor mv, String className, String methodName) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        System.out.println("------------MethodVisitor-----visitCode-------------");


        //插入代码  Log.i("TAG", "com/kpa/compiletheplugpile/SecondActivity---->onCreate");
        mv.visitLdcInsn("TAG");
        mv.visitLdcInsn(className + "---->" + methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "i",
                "(Ljava/lang/String;Ljava/lang/String;)I", false);

        mv.visitInsn(Opcodes.POP);

        mv.visitLdcInsn("TAG");
        mv.visitLdcInsn(className + "---->" + methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "i",
                "(Ljava/lang/String;Ljava/lang/String;)I", false);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {

        }
        super.visitInsn(opcode);
    }
}
