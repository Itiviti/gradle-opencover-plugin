package com.ullink.gradle.opencover

import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.internal.ConventionTask
import groovyx.gpars.GParsPool
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.atomic.AtomicLong

class OpenCover extends ConventionTask {
    def openCoverHome
    def openCoverVersion
    def targetExec
    def targetExecArgs
    def parallelForks
    def parallelTargetExecArgs
    def registerMode
    List targetAssemblies
    def excludeByFile
    def excludeByAttribute
    def hideSkipped
    def coverageReportPath

    boolean returnTargetCode = true
    boolean ignoreFailures = false
    boolean mergeOutput = false
    boolean skipAutoProps = false

    static def fileNameId = new AtomicLong(1)

    OpenCover() {
        conventionMapping.map 'registerMode', { 'user' }

        inputs.files {
            getTargetAssemblies()
        }

        outputs.dir {
            getReportsFolder()
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
        coverageReportPath
    }

    @TaskAction

    def build() {
        reportsFolder.mkdirs()

        runOpenCover()
    }

    def runOpenCover() {
        def commandLineArgs = getCommonOpenCoverArgs()

        if (!getParallelForks() || getParallelTargetExecArgs().size() == 0) {
            coverageReportPath = new File(reportsFolder, "coverage.xml")
            runSingleOpenCover(commandLineArgs)
        } else {
            coverageReportPath = reportsFolder.path.toString() + File.separator + "*.xml"
            runMultipleOpenCovers(commandLineArgs)
        }
    }

    def getCommonOpenCoverArgs() {
        def commandLineArgs = [openCoverConsole, '-mergebyhash']
        if (getRegisterMode()) commandLineArgs += '-register:' + getRegisterMode()
        if (returnTargetCode) commandLineArgs += '-returntargetcode'
        if (mergeOutput) commandLineArgs += '-mergeoutput'
        if (excludeByFile) commandLineArgs += '-excludebyfile:' + excludeByFile
        if (excludeByAttribute) commandLineArgs += '-excludebyattribute:' + excludeByAttribute
        if (skipAutoProps) commandLineArgs += '-skipautoprops'
        if (hideSkipped) commandLineArgs += '-hideskipped:' + hideSkipped

        def filters = getTargetAssemblies().collect { "+[${FilenameUtils.getBaseName(project.file(it).name)}]*" }
        commandLineArgs += '-filter:\\"' + filters.join(' ') + '\\"'

        commandLineArgs += "-target:${getTargetExec()}"
        commandLineArgs += "-targetdir:${project.buildDir}"

        commandLineArgs
    }

    def runSingleOpenCover(ArrayList commandLineArgs) {
        commandLineArgs += ["\"-targetargs:${getTargetExecArgs().collect({ escapeArg(it) }).join(' ')}\""]
        commandLineArgs += "-output:${getCoverageReportPath()}"

        execute(commandLineArgs)
    }

    def runMultipleOpenCovers(ArrayList commandLineArgs) {
        def targetExecArgs = getParallelTargetExecArgs()
        logger.info "Preparing to run ${targetExecArgs.size()} tests..."
        GParsPool.withPool {
            targetExecArgs.eachParallel {
                def fileName = "${fileNameId.getAndIncrement()}"
                logger.info("Filename generated for the ${it} input was ${fileName}")
                def currentTestArgs = ["\"-targetargs:${it.collect({ escapeArg(it) }).join(' ')}\""]
                currentTestArgs += "-output:${new File(reportsFolder, fileName + ".xml")}"
                execute(commandLineArgs + currentTestArgs)
            }
        }
    }

    def escapeArg(arg) {
        arg = arg.toString()
        if (arg.startsWith('"') && arg.endsWith('"'))
            arg = arg.substring(1, arg.length() - 1)
        if (!arg.contains(' ')) return arg
        if (arg.contains('"'))
            throw new IllegalArgumentException("Don't know how to deal with this argument: ${arg}")
        return '\\"' + arg + '\\"'
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