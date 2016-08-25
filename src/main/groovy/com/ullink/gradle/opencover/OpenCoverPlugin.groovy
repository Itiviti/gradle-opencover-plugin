package com.ullink.gradle.opencover

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class OpenCoverPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.withType(OpenCover).whenTaskAdded { OpenCover task ->
            applyOpencoverConventions(task, project)
        }

        Task opencoverTask = project.task('opencover', type: OpenCover)
        opencoverTask.description = 'Executes tests measuring covering with Opencover'
    }

    def applyOpencoverConventions(OpenCover task, Project project) {
        task.conventionMapping.map "openCoverVersion", { '4.6.519' }
        task.conventionMapping.map "openCoverHome", {
            if (System.getenv()['OPENCOVER_HOME']) {
                return System.getenv()['OPENCOVER_HOME']
            }
            def version = task.getOpenCoverVersion()
            downloadOpenCover(project, version)
        }
        if (project.plugins.hasPlugin('msbuild')) {
            task.dependsOn project.tasks.msbuild
            task.conventionMapping.map "targetAssemblies", {
                project.tasks.msbuild.projects.findAll {
                    !(it.key =~ 'test') && it.value.properties.TargetPath
                }.collect {
                    it.value.getProjectPropertyPath('TargetPath')
                }
            }
        }
    }

    File downloadOpenCover(Project project, String version) {
        def dest = new File(new File(project.gradle.gradleUserHomeDir, 'caches'), 'opencover')
        if (!dest.exists()) {
            dest.mkdirs()
        }
        def ret = new File(dest, "opencover-${version}")
        if (!ret.exists()) {
            project.logger.info "Downloading & Unpacking OpenCover ${version}"
            def tmpFile = File.createTempFile("opencover.zip", null)
            new URL(getUrlForVersion(version)).withInputStream { i ->
                tmpFile.withOutputStream {
                    it << i
                }
            }
            project.ant.unzip(src: tmpFile, dest: ret)
        }
        ret
    }

    def getUrlForVersion(def version) {
        if (version <= '4.5.3207') {
            return "https://bitbucket.org/shaunwilde/opencover/downloads/opencover.${version}.zip"
        }
        return "https://github.com/OpenCover/opencover/releases/download/${version}/opencover.${version}.zip"
    }
}
