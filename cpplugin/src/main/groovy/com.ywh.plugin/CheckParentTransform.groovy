package com.ywh.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import groovy.io.FileType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

public class CheckParentTransform extends Transform {

    /**
     * 设置自定义的Transform对应的Task名称，
     * Gradle 在编译的时候，会将这个名称显示在控制台上
     * @return
     */
    @Override
    String getName() {
        return "CheckParentTransform"
    }

    /**
     * 在项目中会有各种各样格式的文件，返回值可以设置CheckParentTransform接受的文件类型
     * CONTENT_CLASS : 只检索.class文件
     * CONTENT_RESOURCES：只检索Java标准资源文件
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 规定自定义Transform检索的范围，
     * PROJECT： 只有项目内容
     * SUB_PROJECTS： 只有子项目
     * EXTERNAL_LIBRARIES：只有外部库
     * TESTED_CODE： 由当前变量(包括依赖项)测试的代码
     * PROVIDED_ONLY： 只提供本地或远程依赖项
     * SUB_PROJECTS_LOCAL_DEPS： 只有子项目的本地依赖项(本地jar)
     * PROJECT_LOCAL_DEPS: 只有项目的本地依赖(本地jar)
     *
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.PROJECT_ONLY
    }

    /**
     * 是否支持增量编译，不需要直接返回false
     * @return
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 最重要的方法,在这个方法中可以获取两个数据流向
     * inputs          :  inputs中传过来的输入流，其中有两种格式，jar包格式,directory（目录格式）
     * outputProvider  :  outputProvider获取输出目录，最后将修改的文件复制到输出目录，这一步必须做，否则报错
     *
     * @param transformInvocation
     * @throws TransformException* @throws InterruptedException* @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        //拿到所有class文件
        Collection<TransformInput> transformInputs = transformInvocation.inputs;
        TransformOutputProvider outputProvider = transformInvocation.outputProvider;

        if (outputProvider != null) {
            outputProvider.deleteAll()
        }
        transformInputs.each { TransformInput transformInput ->
            transformInput.jarInputs.each { JarInput jarInput ->
                File file = jarInput.file
                def dest = outputProvider.getContentLocation(jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                FileUtils.copyFile(file, dest)
            }
            //遍历directoryInputs(文件夹中的class文件) directoryInputs代表着以源码方式参与项目编译的所有目录结构及其目录下的源码文件
            //比如我们手写的类以及R.class、BuildConfig.class以及MainActivity.class等
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                File dir = directoryInput.file
                if (dir) {
                    dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File file ->
                        System.out.println("find class: " + file.name)
                        println '==========CLASS:' + file.name + '=========='
                        // 对Class 文件进行读取与解析
                        ClassReader classReader = new ClassReader(file.bytes)
                        // class 文件写入
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                        // 访问class 文件相应的内容、解析某一个结构就会通知到ClassVisitor的相应方法
                        CheckParentClassVisitor classVisitor = new CheckParentClassVisitor(classWriter)
                        // 依次调用ClassVisitor 接口的各个方法
                        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                        // 将最终修改的字节码以byte数组形式返回
                        byte[] bytes = classWriter.toByteArray()
                        // 通过文件流写入方式覆盖原先的内容，实现class文件的改写
                        FileOutputStream fileOutputStream = new FileOutputStream(file.path)
                        fileOutputStream.write(bytes)
                        fileOutputStream.close()
                    }
                    // 处理完输入之后吧输出传给下一个文件
                    def dest = outputProvider.getContentLocation(
                            directoryInput.name,
                            directoryInput.contentTypes,
                            directoryInput.scopes,
                            Format.DIRECTORY
                    )
                    FileUtils.copyDirectory(directoryInput.file, dest)
                }
            }
        }
    }
}