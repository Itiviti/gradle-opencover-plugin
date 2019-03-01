package com.ullink.gradle.opencover

import com.ullink.gradle.nunit.NUnitTestResultsMerger
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class OpenCoverNUnitPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.withType(OpenCover).whenTaskAdded { OpenCover task ->
            applyNunitDefaults(task, project)
        }
        project.apply plugin: 'com.ullink.opencover'
    }

    def static applyNunitDefaults(OpenCover task, Project project) {
        Task nunit = project.tasks.nunit
        task.doFirst {
            nunit.prepareExecute()
        }
        task.targetExec.set(project.provider { nunit.nunitExec })
        task.targetExecArgs.set(project.provider { nunit.commandArgs.collect { it.toString() } })
        task.parallelForks.set(project.provider { nunit.parallelForks })

        def intermediateNunitResultsPath = new File(nunit.getReportFolderImpl().toString(), 'intemediate-results-nunit')

        task.parallelTargetExecArgs.set(project.provider {
            intermediateNunitResultsPath.mkdirs()
            nunit.getTestInputAsList(nunit.where?.value).collect({nunit.buildCommandArgs(it, new File (intermediateNunitResultsPath, UUID.randomUUID().toString() + ".xml"))})
        })

        task.doLast {
            if (task.parallelForks.get() && !task.parallelTargetExecArgs.get().isEmpty()) {
                def listOfIntermediateFiles = intermediateNunitResultsPath.listFiles()?.toList()
                if (listOfIntermediateFiles){
                    new NUnitTestResultsMerger().merge(listOfIntermediateFiles, nunit.testReportPath)
                    intermediateNunitResultsPath.deleteDir()
                }
            }
        }

        project.afterEvaluate {
            task.inputsOutputsFrom(nunit)
        }
    }
}
