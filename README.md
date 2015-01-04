gradle-opencover-plugin
=======================

A gradle plugin for getting test coverage using OpenCover

# opencover plugin

A base plugin 'opencover' is provided. It will sets up a task named 'opencover' that when called will execute
the OpenCover.exe file of the associated OpenCover version. That task may be configured:

    opencover {
        // optional - defaults to '4.5.1604'
        openCoverVersion
        // optional - defaults to OPENCOVER_HOME env variable if set or to a downloaded opencover home corresponding to
        // the specified openCoverVersion
        openCoverHome
        // mandatory - specifies the test runner executable path (ie 'nunit-console.exe')
        targetExec
        // mandatory - specifies the test runner arguments (associated 'nunit-console.exe' parameters)
        targetExecArgs
        // optional - targetdir specified to OpenCover for the test runner executable
        targetDir
        // mandatory - assemblies to obtain test coverage for
        targetAssemblies
        // optional - defaults to TRUE. OpenCover will return the return code of the test runner executable.
        returnTargetCode
        // optional - defaults to FALSE. Determines the behavior of the task if OpenCover's return code is abnormal
        ignoreFailures
    }

#opencover-nunit plugin

An NUnit ready plugin is also provided: 'opencover-nunit'. It relies on the gradle-nunit-plugin 'nunit' task to
configure the 'opencover' task. Using it, provided the default values suit you, the only setup you'll need is

    opencover {
        targetAssemblies = ...
    }