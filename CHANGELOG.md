# gradle-opencover-plugin changelog

## 1.14
### Changed
* Default OpenCover version is now 4.7.1189
* The plugin is now published on plugins.gradle.org

## 1.13
### Fixed
* Do not evaluate targetAssemblies when applying the plugin

### Changed
* Remove gpars and use parallelStream instead

## 1.11
### Added
* Add threshold parameter for OpenCover. [usage] (https://github.com/OpenCover/opencover/wiki/Usage).
* Support Gradle 5

### Updated
* Migrate from ConventionTask to DefaultTask

## 1.8

### Removed
* The reports generated for parallel OpenCover launches are no longer merged

## 1.7

### Updated
* OpenCover version to 4.6.519

### Fixed
* Escape double quotes in filters

### Added
* Possibility to start several OpenCovers at once
* Exclusion parameters for OpenCover (excludebyfile, excludebyattribute, skipautoprops, hideskipped and skipautoprops)
[usage](https://github.com/OpenCover/opencover/wiki/Usage)

## 1.6
### Fixed
* Do not use relocated artifact for commons-io

## 1.5
### Added
* OpenCover register mode can be set via the `registerMode` parameter according to opencover's [usage](https://github.com/OpenCover/opencover/wiki/Usage). Can be (set to) `null` to disable the parameter

## 1.4
### Fixed
* The proper assembly names are sent to opencover as filters, yielding
expected result files (which don't contain unwanted assemblies)

## 1.3
### Fixed
* Fix breaking change for gradle-nunit-plugin introduced in 1.5

gradle-nunit-plugin version | gradle-opencover-plugin support version
--- | ----------------------
1.6 | >= 1.3
1.5 | not supported
1.4 or below | <= 1.2
