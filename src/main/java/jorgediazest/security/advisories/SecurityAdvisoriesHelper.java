/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package jorgediazest.security.advisories;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.patcher.PatcherUtil;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ReleaseInfo;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.URLCodec;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Díaz
 */
@Component(
	configurationPid = "jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration",
	immediate = true, service = SecurityAdvisoriesHelper.class
)
public class SecurityAdvisoriesHelper {

	public List<Issue> getJiraIssues(String query, int buildNumber)
		throws IOException, PortalException {

		String encodedQuery = URLCodec.encodeURL(query);

		int delta = 0;

		List<Issue> issues = new ArrayList<>();

		while (true) {
			String jiraSearchApi = StringBundler.concat(
				JIRA_URL, "?", JIRA_DELTA_PARAM, String.valueOf(delta),
				JIRA_FIELDS_PARAM, Issue.JQL_FIELDS, JIRA_JQL_PARAM,
				encodedQuery);

			Map<String, Object> responseMap = executeJiraRequest(jiraSearchApi);

			List<Map<String, Object>> responseIssues =
				(List<Map<String, Object>>)responseMap.get("issues");

			if (responseIssues == null) {
				return Collections.emptyList();
			}

			for (Map<String, Object> responseIssue : responseIssues) {
				Issue issue = new Issue(responseIssue, buildNumber);

				issues.add(issue);
			}

			int maxResults = (int)responseMap.get("maxResults");

			int issuesNumber = responseIssues.size();

			if ((issuesNumber < maxResults) || (issuesNumber == 0)) {
				return issues;
			}

			delta += issuesNumber;
		}
	}

	public int getLatestJiraFixpack() {
		return latestJiraFixpack;
	}

	public int getLatestJiraFixpack(int buildNumber, int fixpack)
		throws IOException, PortalException {

		int majorVersion = buildNumber / 1000;
		int minorVersion = (buildNumber % 1000) / 100;

		String version = String.valueOf(
			majorVersion
		).concat(
			PERIOD
		).concat(
			String.valueOf(minorVersion)
		);

		String query = StringBundler.concat(
			"project = LPS AND status = Closed AND resolution in (Completed, ",
			"Fixed) AND fixVersion = \"", version, ".X\" AND \"", version,
			" Fix Pack Version\" > ", String.valueOf(fixpack),
			" AND updated < -1d ORDER BY \"", version,
			" Fix Pack Version\" DESC");

		if (_log.isDebugEnabled()) {
			_log.debug("JIRA query: " + query);
		}

		String encodedQuery = URLCodec.encodeURL(query);

		String jiraSearchApi = StringBundler.concat(
			JIRA_URL, "?", JIRA_EXPAND_NAMES, JIRA_DELTA_PARAM, "0",
			JIRA_MAX_PARAM, "1", JIRA_JQL_PARAM, encodedQuery);

		Map<String, Object> responseMap = executeJiraRequest(jiraSearchApi);

		fillFixPackVersionCustomFieldsMap(responseMap);

		List<Map<String, Object>> issues =
			(List<Map<String, Object>>)responseMap.get("issues");

		if ((issues == null) || (issues.size() != 1)) {
			return 0;
		}

		Map<String, Object> issue = issues.get(0);

		Map<String, Object> fields = (Map<String, Object>)issue.get("fields");

		return _getFixpack(fields, buildNumber);
	}

	public SecurityAdvisoriesConfiguration
		getSecurityAdvisoriesConfiguration() {

		return _securityAdvisoriesConfiguration;
	}

	public List<Issue> getSev1Issues() {
		return sev1Issues;
	}

	public List<Issue> getSev2Issues() {
		return sev2Issues;
	}

	public List<Issue> getSev3Issues() {
		return sev3Issues;
	}

	public boolean hasSecurityAdvisoriesRole(
		HttpServletRequest httpServletRequest) {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		return hasSecurityAdvisoriesRole(themeDisplay.getPermissionChecker());
	}

	public boolean hasSecurityAdvisoriesRole(
		PermissionChecker permissionChecker) {

		if (_roles.length == 0) {
			return permissionChecker.isOmniadmin();
		}

		User user = permissionChecker.getUser();

		for (String role : _roles) {
			try {
				if (roleLocalService.hasUserRole(
						user.getUserId(), user.getCompanyId(), role, true)) {

					return true;
				}
			}
			catch (PortalException e) {
				if (_log.isWarnEnabled()) {
					_log.warn(e);
				}
			}
		}

		return false;
	}

	public class Issue {

		public static final String JQL_FIELDS = "labels,summary,components";

		public Issue(Map<String, Object> issueMap, int buildNumber) {
			key = (String)issueMap.get("key");

			Map<String, Object> fields = (Map<String, Object>)issueMap.get(
				"fields");

			summary = (String)fields.get("summary");

			List<Map<String, Object>> componentsJira =
				(List<Map<String, Object>>)fields.get("components");

			Stream<Map<String, Object>> componentsJiraStream =
				componentsJira.stream();

			components = componentsJiraStream.map(
				componentJira -> (String)componentJira.get("name")
			).filter(
				name -> !name.equals("Security Vulnerability")
			).collect(
				Collectors.toList()
			);

			fixpack = _getFixpack(fields, buildNumber);

			labels = (List<String>)fields.get("labels");

			for (String label : labels) {
				try {
					if (label.startsWith("lsv-")) {
						lsv = Integer.valueOf(label.replaceFirst("lsv-", ""));

						continue;
					}
					else if (label.startsWith("sev-")) {
						sev = Integer.valueOf(label.replaceFirst("sev-", ""));

						continue;
					}

					int labelFixpackNumber = getLabelFixpackNumber(
						label, buildNumber);

					if (fixpack < labelFixpackNumber) {
						fixpack = labelFixpackNumber;
					}
				}
				catch (Exception exception) {
					if (_log.isDebugEnabled()) {
						_log.debug(
							StringBundler.concat(
								exception.toString(), " processing label ",
								label, " in issue ", key));
					}
				}
			}
		}

		public int getFixpack() {
			return fixpack;
		}

		public String getKey() {
			return key;
		}

		public int getLsv() {
			return lsv;
		}

		public int getSev() {
			return sev;
		}

		public String getSummary() {
			return summary;
		}

		public String toString() {
			if (summary.startsWith("LSV-" + lsv) || (lsv == 0) ||
				((sev != 1) && (sev != 2))) {

				return key + " - " + summary;
			}

			return StringBundler.concat(
				key, " - LSV:", String.valueOf(lsv), ": ", summary);
		}

		protected List<String> components;
		protected int fixpack;
		protected String key;
		protected List<String> labels;
		protected int lsv;
		protected int sev;
		protected String summary;

	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_securityAdvisoriesConfiguration = ConfigurableUtil.createConfigurable(
			SecurityAdvisoriesConfiguration.class, properties);

		initRoles();

		reset();

		initialize();

		writeLogTraces();
	}

	protected Map<String, Object> executeJiraRequest(String url)
		throws IOException, PortalException {

		Http.Options options = new Http.Options();

		Map<String, String> headers = Collections.singletonMap(
			"Content-Type", "application/json");

		options.setHeaders(headers);

		options.setLocation(url);
		options.setPost(false);
		options.setTimeout(30000);

		byte[] bytes = http.URLtoByteArray(options);

		Http.Response response = options.getResponse();

		int responseCode = response.getResponseCode();

		String responseJSON = new String(bytes);

		if (_log.isDebugEnabled()) {
			_log.debug("responseCode: " + responseCode);
			_log.debug("responseJSON: " + responseJSON);
		}

		Map<String, Object> responseMap = null;

		String errorMessage = null;

		try {
			responseMap = (Map<String, Object>)jsonFactory.looseDeserialize(
				responseJSON);

			errorMessage = StringUtil.merge(
				(List)responseMap.get("errorMessages"), COMMA_AND_SPACE);
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug("responseJSON:" + responseJSON);
				_log.debug(exception, exception);
			}
		}

		if ((responseCode == 400) && (errorMessage != null) &&
			(errorMessage.contains(
				"does not exist for the field 'fixVersion'") ||
			 errorMessage.contains("Not able to sort using field '"))) {

			return Collections.emptyMap();
		}

		if (responseCode != 200) {
			throw new PortalException(
				StringBundler.concat(
					url, " returned error code ", String.valueOf(responseCode),
					" - ", errorMessage));
		}

		return responseMap;
	}

	protected void fillFixPackVersionCustomFieldsMap(
		Map<String, Object> responseMap) {

		Map<String, String> names = (Map<String, String>)responseMap.get(
			"names");

		if (names == null) {
			return;
		}

		for (Map.Entry<String, String> entry : names.entrySet()) {
			Matcher matcher = fixPackVersionPattern.matcher(entry.getValue());

			if (!matcher.matches()) {
				continue;
			}

			int majorVersion = GetterUtil.getInteger(matcher.group(1), -1);
			int minorVersion = GetterUtil.getInteger(matcher.group(2), -1);

			if ((majorVersion <= 0) || (minorVersion < 0)) {
				continue;
			}

			int build = (majorVersion * 1000) + (minorVersion * 100) + 10;

			fixPackVersionCustomFieldsMap.put(build, entry.getKey());
		}
	}

	protected List<Issue> filterInstalledIssues(
		List<Issue> issues, String[] fixedIssues, int fixpackLevel) {

		Stream<Issue> issuesStream = issues.stream();

		return issuesStream.filter(
			issue -> !ArrayUtil.contains(fixedIssues, issue.getKey())
		).filter(
			issue -> issue.getFixpack() > fixpackLevel
		).collect(
			Collectors.toList()
		);
	}

	protected String getIssueKeys(List<Issue> issues) {
		Stream<Issue> issuesStream = issues.stream();

		return issuesStream.map(
			issue -> issue.getKey()
		).collect(
			Collectors.joining(COMMA_AND_SPACE)
		);
	}

	protected String[] getIssuesToIgnore() {
		String configuration =
			_securityAdvisoriesConfiguration.issuesToIgnore();

		if (Validator.isNull(configuration)) {
			return null;
		}

		String[] issuesToIgnore = configuration.split(",|\\s");

		issuesToIgnore = ArrayUtil.filter(
			issuesToIgnore, r -> Validator.isNotNull(r));

		if (issuesToIgnore.length == 0) {
			return null;
		}

		return issuesToIgnore;
	}

	protected List<Issue> getJiraSecurityIssues(int buildNumber, int severity)
		throws IOException, PortalException {

		int majorVersion = buildNumber / 1000;
		int minorVersion = (buildNumber % 1000) / 100;

		String version = String.valueOf(
			majorVersion
		).concat(
			PERIOD
		).concat(
			String.valueOf(minorVersion)
		);

		String query = StringBundler.concat(
			"project = LPE AND status = Closed AND labels = lsv AND ",
			"resolution in (Completed, Fixed) AND fixVersion = \"", version,
			".X EE\" AND labels = sev-", String.valueOf(severity),
			" ORDER BY \"", version, " Fix Pack Version\" DESC");

		if (_log.isDebugEnabled()) {
			_log.debug("JIRA query: " + query);
		}

		return getJiraIssues(query, buildNumber);
	}

	protected int getLabelFixpackNumber(String fixpackLabel, int buildNumber) {
		fixpackLabel = fixpackLabel.replaceFirst("liferay-fixpack-", "");

		String[] fixpackLabelArray = fixpackLabel.split("\\-");

		if (fixpackLabelArray.length < 3) {
			return 0;
		}

		String fixpackPrefix = fixpackLabelArray[0];

		if (!fixpackPrefix.equals("portal") && !fixpackPrefix.equals("de") &&
			!fixpackPrefix.equals("dxp")) {

			return 0;
		}

		try {
			int fixpackBuildNumber = Integer.valueOf(fixpackLabelArray[2]);

			if (fixpackBuildNumber == buildNumber) {
				return Integer.valueOf(fixpackLabelArray[1]);
			}
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception, exception);
			}
		}

		return 0;
	}

	protected void initialize() {
		if (!_securityAdvisoriesConfiguration.enabled()) {
			return;
		}

		int buildNumber = ReleaseInfo.getBuildNumber();

		if ((buildNumber % 100) < 10) {
			if (_log.isInfoEnabled()) {
				_log.info("BuildNumber: " + buildNumber);
				_log.info("This Liferay installation cannot be patched");
			}

			reset();

			return;
		}

		int installedFixpackLevel = 0;

		for (String installedPatch : PatcherUtil.getInstalledPatches()) {
			installedFixpackLevel = getLabelFixpackNumber(
				installedPatch, buildNumber);

			if (installedFixpackLevel != 0) {
				break;
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug("installed fixpack level: " + installedFixpackLevel);
		}

		initialize(buildNumber, installedFixpackLevel);
	}

	protected void initialize(int buildNumber, int installedFixpackLevel) {
		try {
			String[] fixedIssues = PatcherUtil.getFixedIssues();

			String[] issuesToIgnore = getIssuesToIgnore();

			if (issuesToIgnore != null) {
				fixedIssues = ArrayUtil.append(fixedIssues, issuesToIgnore);
			}

			latestJiraFixpack = getLatestJiraFixpack(
				buildNumber, installedFixpackLevel);
			sev1Issues = filterInstalledIssues(
				getJiraSecurityIssues(buildNumber, 1), fixedIssues,
				installedFixpackLevel);
			sev2Issues = filterInstalledIssues(
				getJiraSecurityIssues(buildNumber, 2), fixedIssues,
				installedFixpackLevel);
			sev3Issues = filterInstalledIssues(
				getJiraSecurityIssues(buildNumber, 3), fixedIssues,
				installedFixpackLevel);
		}
		catch (IOException ioException) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Cannot connect to jira server: " +
						ioException.getMessage());
			}

			reset();
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(portalException, portalException);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn(portalException.getMessage());
			}

			reset();
		}
	}

	protected void initRoles() {
		String[] roles = _securityAdvisoriesConfiguration.roles();

		_roles = ArrayUtil.filter(roles, r -> Validator.isNotNull(r));
	}

	protected void reset() {
		latestJiraFixpack = 0;
		sev1Issues = Collections.emptyList();
		sev2Issues = Collections.emptyList();
		sev3Issues = Collections.emptyList();
	}

	protected void writeLogTraces() {
		ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
			"content.Language", LocaleUtil.getDefault(),
			SecurityAdvisoriesHelper.class);

		if (_log.isWarnEnabled()) {
			_log.warn(language.get(resourceBundle, "disclaimer"));
		}

		if (!sev1Issues.isEmpty()) {
			_log.error(
				language.format(
					resourceBundle, "security-vulnerabilities-not-fixed-x-x",
					new String[] {"SEV-1", getIssueKeys(sev1Issues)}));
			_log.error(
				language.format(
					resourceBundle,
					"security-vulnerabilities-update-to-fixpack-x",
					_getLatestFixpackNumber(sev1Issues)));
		}

		if (!sev2Issues.isEmpty()) {
			_log.error(
				language.format(
					resourceBundle, "security-vulnerabilities-not-fixed-x-x",
					new String[] {"SEV-2", getIssueKeys(sev2Issues)}));
			_log.error(
				language.format(
					resourceBundle,
					"security-vulnerabilities-update-to-fixpack-x",
					_getLatestFixpackNumber(sev2Issues)));
		}

		if (!sev1Issues.isEmpty() || !sev2Issues.isEmpty()) {
			return;
		}

		if (_log.isWarnEnabled() && !sev3Issues.isEmpty()) {
			_log.warn(
				language.format(
					resourceBundle, "security-vulnerabilities-not-fixed-x-x",
					new String[] {"SEV-3", getIssueKeys(sev3Issues)}));
			_log.warn(
				language.format(
					resourceBundle,
					"security-vulnerabilities-update-to-fixpack-x",
					_getLatestFixpackNumber(sev3Issues)));

			return;
		}

		if (_log.isInfoEnabled() && (latestJiraFixpack > 0)) {
			_log.info(
				language.format(
					resourceBundle, "new-fixpack-available-x",
					latestJiraFixpack));
		}
	}

	protected static final String COMMA_AND_SPACE = ", ";

	protected static final String JIRA_DELTA_PARAM = "&startAt=";

	protected static final String JIRA_EXPAND_NAMES = "expand=names";

	protected static final String JIRA_FIELDS_PARAM = "&fields=";

	protected static final String JIRA_JQL_PARAM = "&jql=";

	protected static final String JIRA_MAX_PARAM = "&maxResults=";

	protected static final String JIRA_URL =
		"https://issues.liferay.com/rest/api/2/search";

	protected static final String PERIOD = ".";

	protected static Pattern fixPackVersionPattern = Pattern.compile(
		"(\\d)\\.(\\d) Fix Pack Version");

	protected Map<Integer, String> fixPackVersionCustomFieldsMap =
		new HashMap<>();

	@Reference
	protected Http http;

	@Reference
	protected JSONFactory jsonFactory;

	@Reference
	protected Language language;

	protected int latestJiraFixpack;

	@Reference
	protected RoleLocalService roleLocalService;

	protected List<Issue> sev1Issues;
	protected List<Issue> sev2Issues;
	protected List<Issue> sev3Issues;

	private int _getFixpack(Map<String, Object> fields, int buildNumber) {
		String fixPackVersionCustomField = fixPackVersionCustomFieldsMap.get(
			buildNumber);

		if (fixPackVersionCustomField == null) {
			return 0;
		}

		return GetterUtil.getInteger(fields.get(fixPackVersionCustomField));
	}

	private int _getLatestFixpackNumber(List<Issue> issues) {
		if (latestJiraFixpack > 0) {
			return latestJiraFixpack;
		}

		Stream<Issue> issuesStream = issues.stream();

		return issuesStream.mapToInt(
			issue -> issue.getFixpack()
		).max(
		).orElse(
			0
		);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SecurityAdvisoriesHelper.class);

	private String[] _roles;
	private volatile SecurityAdvisoriesConfiguration
		_securityAdvisoriesConfiguration;

}