package com.ullink.gradle.opencover

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class OpenCoverNUnitPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.withType(OpenCover).whenTaskAdded { OpenCover task ->
            applyOpencoverNunitConventions(task, project)
        }
        project.apply plugin: 'com.ullink.opencover'
    }

    def applyOpencoverNunitConventions(OpenCover task, Project project) {
        Task nunit = project.tasks.nunit
        task.doFirst {
            nunit.prepareExecute()
        }

        task.conventionMapping.map 'targetExec', { nunit.nunitExec }
        task.conventionMapping.map 'targetExecArgs', { nunit.commandArgs }
        task.conventionMapping.map 'parallelForks',{ nunit.parallelForks }
        task.conventionMapping.map 'parallelTargetExecArgs', {
            nunit.getTestInputAsList(nunit.where?.value).collect({nunit.buildCommandArgs(it,  new File(nunit.getReportFolderImpl(), "intermediate-results-nunit").mkdirs())})
        }

        project.afterEvaluate {
            task.dependsOn nunit.dependsOn
            task.inputsOutputsFrom(nunit)
        }
    }
}
