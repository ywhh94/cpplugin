package com.ywh.plugin

import com.android.build.gradle.AppExtension
import com.ywh.plugin.checkbase.Cons
import com.ywh.plugin.checkbase.Utils
import com.ywh.plugin.checkbase.CheckBaseExtension
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project

public class CheckBasePlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = "checkBase"

    static final String GET_MERGE_MANIFEST_TASK = "getMergeManifest"

    @Override
    public void apply(Project project) {
        //扩展，EXTENSION_NAME表示在build.gradle中配置的参数头
        //CheckParentExtension就是里面的参数就是可以配置的具体参数
        def checkParentExtension = project.getExtensions().create(EXTENSION_NAME, CheckBaseExtension)


        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        CheckBaseTransform transform = new CheckBaseTransform();
        android.registerTransform(transform);


        createGetMergeManifestTask(project)
        //解析build.gradle文件之后调用
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project p) {
                //获取build.gradle中扩展配置
                Utils.init(checkParentExtension)
                project.getTasks().matching { it ->
                    //获取manifest的task自动执行，必须要在processMergeManifest之后,
                    // 所以我选择在processReleaseJavaRes/processDebugJavaRes之前来执行getManifest的任务
                    it.name.startsWith('merge') && (it.name.endsWith('ReleaseJavaResource')
                            || it.name.endsWith('DebugJavaResource'))
                }.each { task ->
                    // 任务依赖：执行task之前需要执行dependsOn指定的任务
                    if (task.name == "mergeDebugJavaResource") {
                        task.dependsOn(GET_MERGE_MANIFEST_TASK + "debug")
                    } else if (task.name == "mergeReleaseJavaResource") {
                        task.dependsOn(GET_MERGE_MANIFEST_TASK + "release")
                    }
                }
                project.getTasks().matching { it ->
                    it.name == "transformClassesWith" + Cons.TRANSFORM_NAME + "ForDebug" ||
                            it.name == "transformClassesWith" + Cons.TRANSFORM_NAME + "ForRelease"
                }.each { task ->
                    if (task.name == "transformClassesWith" + Cons.TRANSFORM_NAME + "ForDebug") {
                        task.dependsOn(GET_MERGE_MANIFEST_TASK + "debug")
                    } else if (task.name == "transformClassesWith" + Cons.TRANSFORM_NAME + "ForRelease") {
                        task.dependsOn(GET_MERGE_MANIFEST_TASK + "release")
                    }
                }
            }
        })

    }

    //API 'variantOutput.getProcessResources()' is obsolete and has been replaced with 'variantOutput.getProcessResourcesProvider()'.
    void createGetMergeManifestTask(Project project) {
        if (project.android.hasProperty("applicationVariants")) {
            project.android.applicationVariants.all { variant ->

                variant.outputs.all { output ->
                    project.tasks.create(GET_MERGE_MANIFEST_TASK + variant.name, GetManifestTask.class) {
                        setMergeManifestPath(output.processResources.manifestFile.toString())
                    }
                }

            }
        }
    }
}
