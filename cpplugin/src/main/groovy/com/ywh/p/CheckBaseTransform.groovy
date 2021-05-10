package com.ywh.p

import com.ywh.util.Cons
import com.ywh.util.LogDebug
import com.ywh.util.Utils
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.ywh.util.CheckBaseClassVisitor
import groovy.io.FileType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Transform 主要作用是检索项目编译过程中的所有文件
 */
public class CheckBaseTransform extends Transform {

    private List<String> unJarClazz = new ArrayList<>();
    private LogDebug logDebug = new LogDebug("Transform")

/**
 * Returns the unique name of the transform.
 *
 * <p>This is associated with the type of work that the transform does. It does not have to be
 * unique per variant.
 * 设置自定义的Transform 对应的task名称， Gradle 在编译的时候，会将这个名称显示
 * 在控制台上
 * eg: Task:app:transformClassesWithXXXForDebug
 */
    @Override
    String getName() {
        return Cons.TRANSFORM_NAME
    }

    /**
     * Returns the type(s) of data that is consumed by the Transform. This may be more than
     * one type.
     * 在项目中会有各种各样格式的文件，通过getInputType可以设置
     * CustomLogTransform接收的文件类型，此方法返回的类型是Set<QualifiedContent.ContentType>集合
     * ContentType:
     * CLASSES： 只检索.class文件
     * RESOURCES： 检索Java标准资源文件
     * <strong>This must be of type {@link com.android.build.api.transform.QualifiedContent.DefaultContentType}</strong>
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * Returns the scope(s) of the Transform. This indicates which scopes the transform consumes.
     * 这个方法规定自定义 Transform 检索的范围，具体有以下几种取值：
     * PROJECT： 只有项目内容
     * SUB_PROJECTS： 只有子项目
     * EXTERNAL_LIBRARIES：只有外部库
     * TESTED_CODE： 由当前变量(包括依赖项)测试的代码
     * PROVIDED_ONLY： 只提供本地或远程依赖项
     * SUB_PROJECTS_LOCAL_DEPS： 只有子项目的本地依赖项(本地jar)
     * PROJECT_LOCAL_DEPS: 只有项目的本地依赖(本地jar)
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * Returns whether the Transform can perform incremental work.
     *
     * <p>If it does, then the TransformInput may contain a list of changed/removed/added files, unless
     * something else triggers a non incremental run.
     * 是否支持增量编译 不需要直接返回false
     */
    @Override
    boolean isIncremental() {
        return false
    }


    /**
     * 自定义时最重要的方法，在这个方法中可以获取两个数据的流向
     * inputs: inputs 中传过来的输入流，其中有两种格式，jat包格式，directory（目录格式）
     * outputProvider：outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做，否则编译会报错。
     * @param transformInvocation
     * @throws com.android.build.api.transform.TransformException* @throws InterruptedException* @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        // 拿到所有的class文件
        Collection<TransformInput> transformInputs = transformInvocation.inputs
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        if (outputProvider != null) {
            outputProvider.deleteAll()
        }

        transformInputs.each { TransformInput transformInput ->

            handleJar(outputProvider, transformInput)

            handleClazz(outputProvider, transformInput)

        }
        handleUnJarClazz()
        Utils.check()
    }

    //处理jar
    void handleJar(TransformOutputProvider outputProvider, TransformInput transformInput) {
        transformInput.jarInputs.each { JarInput jarInput ->
            File file = jarInput.file
            logDebug.log("findJar", file.getAbsolutePath())

            def dest = outputProvider.getContentLocation(jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes, Format.JAR)
            FileUtils.copyFile(file, dest)

            //比如有一个annotation的module,满足SUB_PROJECTS也可以找到一个jar包,但是解压有MANIFEST.MF无法访问导致报错
            //所以还是加个class.jar
            if (jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS)
                    && file.name == "classes.jar") {
                decompress(file.getAbsolutePath())
            }
//            if (file.name == "classes.jar") {
//                  decompress(file.getAbsolutePath())
//            }
        }
    }

    //处理class,加入到Utils的map中
    void handleClazz(TransformOutputProvider outputProvider, TransformInput transformInput) {
        // // 遍历directoryInputs(文件夹中的class文件) directoryInputs代表着以源码方式参与项目编译的所有目录结构及其目录下的源码文件
        // // 比如我们手写的类以及R.class、BuildConfig.class以及MainActivity.class等
        transformInput.directoryInputs.eachWithIndex { DirectoryInput directoryInput, index ->
            File dir = directoryInput.file
            if (dir) {
                dir.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File file ->
                    logDebug.log("findClass", file.name)
                    // 对Class 文件进行读取与解析
                    ClassReader classReader = new ClassReader(file.bytes)
//                    System.out.println("ClassReader:" + classReader.getSuperName())
                    // class 文件写入
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                    // 访问class 文件相应的内容、解析某一个结构就会通知到ClassVisitor的相应方法
                    CheckBaseClassVisitor classVisitor = new CheckBaseClassVisitor(classWriter)
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
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
    }

    //处理classes.jar解压之后的class
    void handleUnJarClazz() {
        //Jar解压出来的class
        for (int i = 0; i < unJarClazz.size(); i++) {
            File f = new File(unJarClazz.get(i));
            if (!f.exists()) {
                continue
            }
            ClassReader classReader = new ClassReader(f.bytes)
            // class 文件写入
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
            // 访问class 文件相应的内容、解析某一个结构就会通知到ClassVisitor的相应方法
            CheckBaseClassVisitor classVisitor = new CheckBaseClassVisitor(classWriter)
            // 依次调用ClassVisitor 接口的各个方法
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
            f.delete()
        }
    }

    //解压classes.jar 到所属module中build\tmp\tmp_jar_unzip文件中
    private void decompress(String fileName) throws IOException {
        int build = fileName.indexOf("build")
        String outputPath = fileName.substring(0, build + "build".length()) + "\\tmp" + File.separator + "tmp_jar_unzip";
        if (!outputPath.endsWith(File.separator)) {
            outputPath += File.separator;
        }
        JarFile jf = new JarFile(fileName);
        for (Enumeration e = jf.entries(); e.hasMoreElements();) {
            JarEntry je = (JarEntry) e.nextElement();
            String outFileName = outputPath + je.getName();
            String className = je.getName().substring(je.getName().lastIndexOf("/") + 1, je.getName().length());
            if ("BuildConfig.class" == className) {
                continue
            }
            unJarClazz.add(outFileName)
            File f = new File(outFileName);
            logDebug.log("unJar", f.getAbsolutePath())
            //创建该路径的目录和所有父目录
            makeSupDir(outFileName);

            //如果是目录，则直接进入下一个循环
            if (f.isDirectory()) {
                continue;
            }

            InputStream ins = null;
            OutputStream out = null;

            try {
                ins = jf.getInputStream(je);
                out = new BufferedOutputStream(new FileOutputStream(f));
                byte[] buffer = new byte[2048];
                int nBytes = 0;
                while ((nBytes = ins.read(buffer)) > 0) {
                    out.write(buffer, 0, nBytes);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace()
            } finally {
                try {
                    if (null != out) {
                        out.flush();
                        out.close();
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace()
                }
                finally {
                    if (null != ins) {
                        ins.close();
                    }
                }
            }
        }
    }

    /**
     * 循环创建父目录
     * @param outFileName
     */
    private void makeSupDir(String outFileName) {
        //匹配分隔符
        Pattern p = Pattern.compile("[/\\" + File.separator + "]");
        Matcher m = p.matcher(outFileName);
        //每找到一个匹配的分隔符，则创建一个该分隔符以前的目录
        while (m.find()) {
            int index = m.start();
            String subDir = outFileName.substring(0, index);
            File subDirFile = new File(subDir);
            if (!subDirFile.exists())
                subDirFile.mkdir();
        }
    }

}