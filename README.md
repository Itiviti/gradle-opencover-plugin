gradle-opencover-plugin [![Build status](https://ci.appveyor.com/api/projects/status/chcaikhiapb4mmn2?svg=true)](https://ci.appveyor.com/project/gluck/gradle-opencover-plugin) [![Build Status](https://travis-ci.org/Ullink/gradle-opencover-plugin.svg?branch=master)](https://travis-ci.org/Ullink/gradle-opencover-plugin) [![GitHub license](https://img.shields.io/github/license/Ullink/gradle-opencover-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)
=======================

A gradle plugin for getting test coverage using OpenCover

# opencover plugin

A base plugin 'com.ullink.opencover' is provided. It will sets up a task named 'opencover' that when called will execute
the OpenCover.exe file of the associated OpenCover version. That task may be configured:

    opencover {
        // optional - defaults to '4.6.166'
        openCoverVersion
        // optional - defaults to OPENCOVER_HOME env variable if set or to a downloaded opencover home corresponding to
        // the specified openCoverVersion
        openCoverHome
        // mandatory - specifies the test runner executable path (ie 'nunit-console.exe')
        targetExec
        // mandatory - specifies the test runner arguments (associated 'nunit-console.exe' parameters)
        targetExecArgs
        // mandatory - assemblies to obtain test coverage for
        targetAssemblies
        // optional - defaults to TRUE. OpenCover will return the return code of the test runner executable.
        returnTargetCode
        // optional - defaults to FALSE. Determines the behavior of the task if OpenCover's return code is abnormal
        ignoreFailures
    }

#opencover-nunit plugin

An NUnit ready plugin is also provided: 'com.ullink.opencover-nunit'. It relies on the gradle-nunit-plugin 'nunit' task to
configure the 'opencover' task. Using it, provided the default values suit you, the only setup you'll need is

    opencover {
        targetAssemblies = ...
    }

# License

All these plugins are licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) with no warranty (expressed or implied) for any purpose.
