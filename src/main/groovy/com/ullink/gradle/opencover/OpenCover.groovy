package com.ullink.gradle.opencover

import org.apache.commons.io.FilenameUtils
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.internal.ConventionTask
import groovyx.gpars.GParsPool
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

class OpenCover extends ConventionTask {
    def openCoverHome
    def openCoverVersion
    def targetExec
    def targetExecArgs
    def parallelForks
    def parallelTargetExecArgs
    def intermediateNunitResultsPath
    def registerMode
    List targetAssemblies
    def excludeByFile
    def excludeByAttribute
    def hideSkipped
    def reportGeneratorVersion
    def assemblyFilters

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

    def getReportGeneratorConsole() {
        assert getReportGeneratorHome(), "You must install ReportGenerator to merge intermediate results"
        File reportGeneratorExec = new File(getReportGeneratorHome().toString(), "ReportGenerator.exe")
        reportGeneratorExec
    }

    def getReportGeneratorHome() {
        def reportGenerator = 'reportgenerator-' + reportGeneratorVersion
        Paths.get(project.gradle.gradleUserHomeDir.toString(), 'caches', 'reportgenerator', reportGenerator)
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

        runOpenCover()
    }

    def runOpenCover() {
        def commandLineArgs = getCommonOpenCoverArgs()

        if (!getParallelForks() || getParallelTargetExecArgs().size() == 0) {
            runSingleOpenCover(commandLineArgs)
        }
        else {
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

        commandLineArgs += "-target:${getTargetExec()}"
        commandLineArgs += "-targetdir:${project.buildDir}"

        commandLineArgs
    }

    def runSingleOpenCover(ArrayList commandLineArgs) {
        commandLineArgs += ["\"-targetargs:${getTargetExecArgs().collect({escapeArg(it)}).join(' ')}\""]
        def filters = getTargetAssemblies().collect { "+[${FilenameUtils.getBaseName(project.file(it).name)}]*" }
        commandLineArgs += '-filter:\\"' + filters.join(' ') + '\\"'
        commandLineArgs += "-output:${getCoverageReportPath()}"

        execute(commandLineArgs)
    }

    def runMultipleOpenCovers(ArrayList commandLineArgs) {
        def intermediateReportsPath = new File(reportsFolder, "intermediate-results-" + name)
        intermediateReportsPath.mkdirs()

        def nunitIntermediateFilesPath = new File(getIntermediateNunitResultsPath(), "")
        nunitIntermediateFilesPath.mkdirs()

        GParsPool.withPool {
            getParallelTargetExecArgs().eachParallel {
                def fileName = getRandomFileName()
                logger.info("Filename generated for the ${it} input was ${fileName}")

                commandLineArgs += ["\"-targetargs:${it.collect({escapeArg(it)}).join(' ')}\""]
                commandLineArgs += "-output:${new File(intermediateReportsPath, fileName + ".xml")}"

                execute(commandLineArgs)
            }
        }

        execute(getMergeCommand(intermediateReportsPath))

        new File(reportsFolder, "Summary.xml").renameTo(new File(reportsFolder, "coverage.xml"))
        new File(intermediateReportsPath, "").deleteDir()
    }

    def getRandomFileName() {
        UUID.randomUUID().toString()
    }

    def escapeArg(arg) {
        arg = arg.toString()
        if (arg.startsWith('"') && arg.endsWith('"'))
            arg = arg.substring(1, arg.length()-1)
        if (!arg.contains(' ')) return arg
        if (arg.contains('"'))
            throw new IllegalArgumentException("Don't know how to deal with this argument: ${arg}")
        return '\\"'+arg+'\\"'
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

    def getMergeCommand(File intermediateReportsPath) {
        def mergeCommand = [getReportGeneratorConsole()]
        mergeCommand += "\"-reports:${intermediateReportsPath}\\*.xml\""
        mergeCommand += "\"-targetdir: ${reportsFolder}\""
        mergeCommand += "-assemblyfilters:+${getAssemblyFilters()}"
        mergeCommand += "-reporttypes:xmlSummary"

        mergeCommand
    }
}