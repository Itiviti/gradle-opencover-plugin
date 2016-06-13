# gradle-opencover-plugin changelog

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
