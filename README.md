#  CheckParent插件
    检查所有Activity是否都继承BaseActivity

plugins {
    id 'com.android.application'
    id 'style-checkbase'
}

checkBase {
    throwException = false
    exceptionAtOnce = false
    openLog = true
}

1.使用：
    在指定BaseActivity添加@BaseActivity注解
2.编译期
    2.1.解析AndroidManifest.xml(合成之后的),获取所有Activity
    2.2.找到所有class,key:className,value:parentName
        获取@BaseActivity/@IgnoreActivity 注解的Activity,
    2.3.开始检查。
        
        2.1解析AndroidManifest.xml 需要保证在build后面来执行，就是 合成的AndroidManifest.xml生成之后


app中class可以直接找到class
其他module的class,会打包classes.jar
引入的第三方包就是比如lifecycle-runtime-2.1.0-runtime.jar

比如我们有module
login/base
E:\myspace\PluginCp\base\build\intermediates\runtime_library_classes_jar\debug\classes.jar
E:\myspace\PluginCp\login\build\intermediates\runtime_library_classes_jar\debug\classes.jar

我们解压各个module的classes.jar文件
得到.class文件列表，在遍历判断


无法判断一个class是否是Activity,先暂停了