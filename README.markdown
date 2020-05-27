# Security Advisories for Liferay DXP (unofficial)

## Disclaimer

**Disclaimer**: This application is not officially supported by Liferay, Inc. or its affiliates.

In case you have any question about any Liferay security vulnerability, you should always check the official Liferay Security Advisories:

  - Liferay DXP (enterprise): https://help.liferay.com/hc/en-us/articles/360018875952-Security-Advisories
  - Liferay Portal (community): https://portal.liferay.dev/learn/security/known-vulnerabilities/-/asset_publisher/HbL5mxmVrnXW

I am providing this tool to make easier to notice if your system is out-of-date but <ins>you should always subscribe and review the official Liferay vulnerability notifications</ins>

## Introduction

Security Advisories application displays the security vulnerabilities that are not fixed in your Liferay DXP installation.

Once it is installed, it will display an alert to the administration users with the vulnerabilities that are not fixed, every time they log in.
This information is also displayed in the Server Administration section and in a trace in the log file during startup.

The vulnerabilities information is retrieved once from JIRA servers, during application startup and it is refreshed every 3 days.

## Supported versions

Security Advisories works in:
  - Liferay DXP 7.0
  - Liferay DXP 7.1
  - Liferay DXP 7.2

Liferay Portal (community edition) is not supported for now.

## Contributing

If you want to contribute to this project, read [CONTRIBUTING.markdown](CONTRIBUTING.markdown) before sending a pull request.

## Installation

Download it from https://github.com/jorgediaz-lr/security-advisories/releases/download/v1.0.0/jorgediazest.security.advisories.jar and copy it to Liferay DXP deploy folder.

Your server must have access to the JIRA server https://issues.liferay.com in order to be able to download the information of solved security issues and fixpacks from there. (Note: As solved LPE bug information is open, a JIRA user account is not needed)

## Usage

This application just adds an alert message that will be displayed in several places:
  - Trace in the log file during startup
  - Notification after you log in
  - Server administration page

Depending on the status of your server, you will see different messages; see following sections:

### Error message: The server has a critical vulnerability :heavy_exclamation_mark:

If your server has a critical vulnerability not fixed (SEV-1 or SEV-2 levels) an error message will be displayed:
  - Log file

![](images/error_log-file.png)

  - Login notification

![](images/error_notification.png)

  - Server administration page

![](images/error_server-admin.png)

### Warning message: The server has a minor vulnerability :warning:

If your server has a minor vulnerability not fixed (SEV-3 level) a warning message will be displayed:
  - Log file

![](images/warn_log-file.png)

  - Login notification

![](images/warn_notification.png)

  - Server administration page

![](images/warn_server-admin.png)

### Information message: There aren't vulnerabilities, but there is a newer fixpack available :information_source:

If there aren't any vulnerabilities to fix, but there is a newer fixpack available, an information  message will be displayed:
  - Log file

![](images/info_log-file.png)

  - Login notification

![](images/info_notification.gif)

  - Server administration page

![](images/info_server-admin.png)

### No messages: The server is up-to-date :relaxed:

If your server is up-to-date nothing will be displayed anywhere, you don't have to install any fixpacks.

Relax and wait until next vulnerability is fixed and published in a new fixpack :relaxed: