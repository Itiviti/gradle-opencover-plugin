package com.ullink.gradle.opencover

import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class OpenCover extends ConventionTask {
    def openCoverHome
    def openCoverVersion
    def targetExec
    def targetExecArgs
    List targetAssemblies

    boolean returnTargetCode = true
    boolean ignoreFailures = false
    boolean mergeOutput = false

    OpenCover() {
        inputs.files {
            getTargetAssemblies()
        }
        outputs.files {
            getCoverageReportPath()
        }
    }

    def inputsOutputsFrom(Task task) {
        inputs.files {
            task.inputs.files
        }
        outputs.files {
            task.outputs.files
        }
    }

    def getOpenCoverConsole() {
        assert getOpenCoverHome(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        File openCoverExec = new File(project.file(getOpenCoverHome()), "OpenCover.Console.exe")
        assert openCoverExec.isFile(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        openCoverExec
    }

    def getOutputFolder() {
        new File(project.buildDir, 'opencover')
    }

    def getReportsFolder() {
        new File(outputFolder, 'reports')
    }

    def getCoverageReportPath() {
        new File(reportsFolder, 'coverage.xml')
    }

    @TaskAction
    def build() {
        reportsFolder.mkdirs()

        def commandLine = buildCommandLine()

        execute(commandLine)
    }

    def buildCommandLine() {
        def commandLineArgs = [openCoverConsole, '-register:user', '-mergebyhash']
        if (returnTargetCode) commandLineArgs += '-returntargetcode'
        if (mergeOutput) commandLineArgs += '-mergeoutput'
        commandLineArgs += ["-target:${getTargetExec()}", "-targetargs:${getTargetExecArgs().join(' ')}", "-targetdir:${project.buildDir}"]
        getTargetAssemblies().each {
            commandLineArgs += "+[${FilenameUtils.getBaseName(project.file(it).name)}]"
        }
        commandLineArgs += "-output:${getCoverageReportPath()}"
    }

    def execute(commandLineArgs) {
        def mbr = project.exec {
            commandLine = commandLineArgs
            ignoreExitValue = ignoreFailures
        }

        if (!ignoreFailures && mbr.exitValue < 0) {
            throw new GradleException("${openCoverConsole} execution failed (ret=${mbr.exitValue})");
        }
    }
}
