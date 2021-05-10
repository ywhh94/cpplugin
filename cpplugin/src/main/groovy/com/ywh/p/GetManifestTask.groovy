package com.ywh.p

import com.ywh.util.LogDebug
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.ywh.util.Utils

public class GetManifestTask extends DefaultTask {

    private String mergeManifestPath

    void setMergeManifestPath(String mergeManifestPath) {
        this.mergeManifestPath = mergeManifestPath
    }

    String getMergeManifestPath() {
        return mergeManifestPath
    }

    @TaskAction
    void start() {
        LogDebug logDebug = new LogDebug("ManifestTask")

        project.android.applicationVariants.all { variant ->
            if (new File(getMergeManifestPath()).exists()) {
                //防止task执行多次（不知道为什么多次）
                if (Utils.getManifestAcSize() == 0) {
                    def manifestContent = new java.io.File(getMergeManifestPath()).getText()
                    logDebug.log("Manifest", getMergeManifestPath().toString())
                    def xml = new XmlParser().parseText(manifestContent)
                    xml.application.activity.each {
                        logDebug.log("Activity", it.toString())
                        logDebug.log("Attributes", it.attributes())
                        Utils.putManifestActivity(it.attributes().toString())
                    }
                }
            } else {
                logDebug.log(getMergeManifestPath() + ":not exist")
            }
        }
    }
}
