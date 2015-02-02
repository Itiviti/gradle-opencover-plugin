package com.ullink.gradle.opencover

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class OpenCoverNUnitPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.withType(OpenCover).whenTaskAdded { OpenCover task ->
            applyOpencoverNunitConventions(task, project)
        }
        project.apply plugin: 'opencover'
    }

    def applyOpencoverNunitConventions(OpenCover task, Project project) {
        Task nunit = project.tasks.nunit
        task.doFirst {
            nunit.prepareExecute()
        }
        task.conventionMapping.map 'targetExec', { nunit.nunitExec }
        task.conventionMapping.map 'targetExecArgs', { nunit.buildCommandArgs() }
        project.afterEvaluate {
            task.dependsOn nunit.dependsOn
            task.inputsOutputsFrom(nunit)
        }
    }
}
