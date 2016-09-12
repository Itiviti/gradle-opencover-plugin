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
    def registerMode
    List targetAssemblies
    def excludeByFile
    def excludeByAttribute
    def hideSkipped

    boolean returnTargetCode = true
    boolean ignoreFailures = false
    boolean mergeOutput = false
    boolean skipAutoProps = false

    OpenCover() {
        conventionMapping.map 'registerMode', { 'user' }

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
        def commandLineArgs = [openCoverConsole, '-mergebyhash']
        if (getRegisterMode()) commandLineArgs += '-register:' + getRegisterMode()
        if (returnTargetCode) commandLineArgs += '-returntargetcode'
        if (mergeOutput) commandLineArgs += '-mergeoutput'
        if (excludeByFile) commandLineArgs += '-excludebyfile:' + excludeByFile
        if (excludeByAttribute) commandLineArgs += '-excludebyattribute:' + excludeByAttribute
        if (skipAutoProps) commandLineArgs += '-skipautoprops'
        if (hideSkipped) commandLineArgs += '-hideskipped:' + hideSkipped

        commandLineArgs += ["-target:${getTargetExec()}", "\"-targetargs:${getTargetExecArgs().collect({escapeArg(it)}).join(' ')}\"", "-targetdir:${project.buildDir}"]
        def filters = getTargetAssemblies().collect { "+[${FilenameUtils.getBaseName(project.file(it).name)}]*" }
        commandLineArgs += '-filter:\\"' + filters.join(' ') + '\\"'
        commandLineArgs += "-output:${getCoverageReportPath()}"
    }

    def escapeArg(arg) {
        arg = arg.toString()
        if (arg.startsWith('"') && arg.endsWith('"'))
            arg = arg.substring(1, arg.length()-1)
        if (!arg.contains(' ')) return arg
        if (arg.contains('"'))
            throw new IllegalArgumentException("Don't know how to deal with this argument: ${arg}")
        return '\\"'+arg+'\\"';
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
