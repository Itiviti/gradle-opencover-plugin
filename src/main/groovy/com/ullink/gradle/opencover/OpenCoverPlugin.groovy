package com.ullink.gradle.opencover

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class OpenCoverPlugin implements Plugin<Project> {
    String DEFAULT_OPENCOVER_VERSION = '4.7.1189'
    void apply(Project project) {
        project.tasks.withType(OpenCover).whenTaskAdded { OpenCover task ->
            applyDefaults(task, project)
        }

        Task task = project.task('opencover', type: OpenCover)
        task.description = 'Executes tests measuring covering with Opencover'
    }

    def applyDefaults(OpenCover task, Project project) {
        task.openCoverVersion.set(project.provider { DEFAULT_OPENCOVER_VERSION })
        task.openCoverHome.set(project.provider {
            def home = System.getenv()['OPENCOVER_HOME']
            if (home) {
                return home
            }
            def version = task.openCoverVersion.get()
            downloadOpenCover(project, version)
        })

        project.plugins.withId('com.ullink.msbuild') {
            def msbuildTask = project.tasks.msbuild
            task.targetAssemblies.set(project.provider {
                msbuildTask.projects.findAll {
                    !(it.key =~ 'test') && it.value.properties.TargetPath
                }.collect {
                    project.file(it.value.getProjectPropertyPath('TargetPath').toString())
                }
            })
        }
    }

    String downloadOpenCover(Project project, String version) {
        def dest = new File(new File(project.gradle.gradleUserHomeDir, 'caches'), 'opencover')

        if (!dest.exists()) {
            dest.mkdirs()
        }
        def ret = new File(dest, "opencover-${version}")
        if (!ret.exists()) {
            project.logger.info "Downloading & Unpacking OpenCover ${version}"
            def tmpFile = File.createTempFile("opencover.zip", null)
            new URL(getOpenCoverUrl(version)).withInputStream { i ->
                tmpFile.withOutputStream {
                    it << i
                }
            }
            project.ant.unzip(src: tmpFile, dest: ret)
        }
        ret.path
    }

    def getOpenCoverUrl(def version) {
        if (version <= '4.5.3207') {
            return "https://bitbucket.org/shaunwilde/opencover/downloads/opencover.${version}.zip"
        }
        return "https://github.com/OpenCover/opencover/releases/download/${version}/opencover.${version}.zip"
    }
}
