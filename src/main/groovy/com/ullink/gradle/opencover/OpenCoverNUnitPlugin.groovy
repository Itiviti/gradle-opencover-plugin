package com.ullink.gradle.opencover

import com.ullink.gradle.nunit.NUnitTestResultsMerger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import com.ullink.gradle.*

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

        def intermediateNunitResultsPath = new File(nunit.getReportFolderImpl().toString() , "intemediate-results-nunit")

        task.conventionMapping.map 'parallelTargetExecArgs', {
            intermediateNunitResultsPath.mkdirs()
            nunit.getTestInputAsList(nunit.where?.value).collect({nunit.buildCommandArgs(it,  new File (intermediateNunitResultsPath,  UUID.randomUUID().toString() + ".xml"))})

        }

        task.doLast {
            def listOfIntermediateFiles = intermediateNunitResultsPath.listFiles().toList()
            if (!listOfIntermediateFiles?.empty){
                new NUnitTestResultsMerger().merge(listOfIntermediateFiles, nunit.testReportPath)
                intermediateNunitResultsPath.deleteDir()
            }
        }

        project.afterEvaluate {
            task.dependsOn nunit.dependsOn
            task.inputsOutputsFrom(nunit)
        }
    }
}
