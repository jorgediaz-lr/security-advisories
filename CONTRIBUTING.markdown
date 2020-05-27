# How to Contribute

If you want to contribute to "security advisories" these guidelines will be important for you.

## Setup

Execute `gradlew eclipse` or `gradlew idea` to setup the project for your IDE

## Pull requests

You can send me your pull requests with your contributions.

You have to follow this guidelines:
 - Code must work in Liferay DXP 7.0 version
 - The code must follow Liferay source formating: execute `gradlew formatSource` before sending any pull request

# Releasing

Execute `gradlew build` to build the module. JAR file will be generated in `build/libs` folder.

By default, this project compiles using Liferay 7.0 dependencies

If you want to release it for a different Liferay version, modify following lines of `build.gradle` file:
```
dependencies {
	targetPlatformBoms group: "com.liferay.portal", name: "release.portal.bom", version: "7.0.6"
	targetPlatformBoms group: "com.liferay.portal", name: "release.portal.bom.compile.only", version: "7.0.6"
}
```
You can use the versions available here: https://repository.liferay.com/nexus/index.html#nexus-search;quick~release.portal.bom