# gradle-opencover-plugin changelog

## 1.7

### Updated
* OpenCover version to 4.6.519

### Fixed
* Escape double quotes in filters

### Added
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
