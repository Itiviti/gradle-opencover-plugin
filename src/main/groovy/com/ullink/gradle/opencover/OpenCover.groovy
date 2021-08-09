package com.ullink.gradle.opencover

import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.plugins.ide.eclipse.model.Output

import java.util.concurrent.atomic.AtomicLong

class OpenCover extends DefaultTask {
    @Input
    final Property<String> openCoverHome
    @Input
    final Property<String> openCoverVersion
    @Optional
    @Input
    final Property<File> targetExec
    @Optional
    @Input
    final ListProperty<String> targetExecArgs
    @Optional
    @Input
    final Property<Boolean> parallelForks
    @Optional
    @Input
    final ListProperty<String> parallelTargetExecArgs
    @Optional
    @Input
    final Property<String> registerMode

    @InputFiles
    ListProperty<File> targetAssemblies
    @Optional
    @Input
    def excludeByFile
    @Optional
    @Input
    def excludeByAttribute
    @Optional
    @Input
    def hideSkipped
    @Internal
    def coverageReportPath
    @Optional
    @Input
    def threshold

    @Input
    boolean returnTargetCode = true
    @Input
    boolean ignoreFailures = false
    @Input
    boolean mergeOutput = false
    @Input
    boolean skipAutoProps = false

    static def fileNameId = new AtomicLong(1)

    OpenCover() {
        openCoverHome = project.getObjects().property(String)
        openCoverVersion = project.getObjects().property(String)
        registerMode = project.getObjects().property(String)
        targetExec = project.getObjects().property(File)
        targetExecArgs = project.getObjects().listProperty(String)
        parallelForks = project.getObjects().property(Boolean)
        parallelTargetExecArgs = project.getObjects().listProperty(String)
        targetAssemblies = project.getObjects().listProperty(File)

        registerMode.set('user')
    }

    def inputsOutputsFrom(Task task) {
        inputs.files {
            task.inputs.files
        }
        outputs.files {
            task.outputs.files
        }
    }

    @Internal
    def getOpenCoverConsole() {
        assert getOpenCoverHome(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        File openCoverExec = new File(project.file(getOpenCoverHome()), "OpenCover.Console.exe")
        assert openCoverExec.isFile(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        openCoverExec
    }

    @Internal
    def getOutputFolder() {
        new File(project.buildDir, 'opencover')
    }

    @OutputDirectory
    def getReportsFolder() {
        new File(outputFolder, 'reports')
    }

    @TaskAction
    def build() {
        reportsFolder.mkdirs()

        runOpenCover()
    }

    def runOpenCover() {
        def commandLineArgs = getCommonOpenCoverArgs()

        if (!parallelForks.get() || parallelTargetExecArgs.get().isEmpty()) {
            coverageReportPath = new File(reportsFolder, "coverage.xml")
            runSingleOpenCover(commandLineArgs)
        } else {
            coverageReportPath = reportsFolder.path.toString() + File.separator + "*.xml"
            runMultipleOpenCovers(commandLineArgs)
        }
    }

    @Internal
    def getCommonOpenCoverArgs() {
        def commandLineArgs = [openCoverConsole, '-mergebyhash']
        if (registerMode.get()) commandLineArgs += '-register:' + registerMode.get()
        if (returnTargetCode) commandLineArgs += '-returntargetcode'
        if (mergeOutput) commandLineArgs += '-mergeoutput'
        if (excludeByFile) commandLineArgs += '-excludebyfile:' + excludeByFile
        if (excludeByAttribute) commandLineArgs += '-excludebyattribute:' + excludeByAttribute
        if (skipAutoProps) commandLineArgs += '-skipautoprops'
        if (hideSkipped) commandLineArgs += '-hideskipped:' + hideSkipped
        if (threshold) commandLineArgs += '-threshold:' + threshold

        def filters = targetAssemblies.get().collect { "+[${FilenameUtils.getBaseName(it.name)}]*" }
        commandLineArgs += '-filter:\\"' + filters.join(' ') + '\\"'

        commandLineArgs += "-target:${targetExec.get()}"
        commandLineArgs += "-targetdir:${project.buildDir}"

        commandLineArgs
    }

    def runSingleOpenCover(ArrayList commandLineArgs) {
        commandLineArgs += ["\"-targetargs:${targetExecArgs.get().collect({ escapeArg(it) }).join(' ')}\""]
        commandLineArgs += "-output:${coverageReportPath}"

        execute(commandLineArgs)
    }

    def runMultipleOpenCovers(ArrayList commandLineArgs) {
        def execArgs = parallelTargetExecArgs.get()
        logger.info "Preparing to run ${execArgs.size()} tests..."

        execArgs.parallelStream()
            .forEach {
                def fileName = "${fileNameId.getAndIncrement()}"
                logger.info("Filename generated for the ${it} input was ${fileName}")
                def currentTestArgs = ["\"-targetargs:${it.collect({ escapeArg(it) }).join(' ')}\""]
                currentTestArgs += "-output:${new File(reportsFolder, fileName + ".xml")}"
                execute(commandLineArgs + currentTestArgs)
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
            throw new GradleException("${openCoverConsole} execution failed (ret=${mbr.exitValue})")
        }
    }
}