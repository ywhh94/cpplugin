package com.ywh.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class CheckParentPlugin implements Plugin<Project> {
    void apply(Project project) {
        println '==========CheckParentPlugin=========='
        def android = project.extensions.getByType(AppExtension)
        println '==========registerTransform=========='
        CheckParentTransform transform = new CheckParentTransform()
        android.registerTransform(transform)
    }
}