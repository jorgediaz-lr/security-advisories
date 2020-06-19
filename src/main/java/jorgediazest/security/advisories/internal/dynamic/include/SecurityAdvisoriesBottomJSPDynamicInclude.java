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

package jorgediazest.security.advisories.internal.dynamic.include;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.taglib.BaseJSPDynamicInclude;
import com.liferay.portal.kernel.servlet.taglib.DynamicInclude;
import com.liferay.portal.kernel.util.GetterUtil;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import jorgediazest.security.advisories.SecurityAdvisoriesHelper;
import jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration;
import jorgediazest.security.advisories.constants.SecurityAdvisoriesWebKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Díaz
 */
@Component(immediate = true, service = DynamicInclude.class)
public class SecurityAdvisoriesBottomJSPDynamicInclude
	extends BaseJSPDynamicInclude {

	@Override
	public void include(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String key)
		throws IOException {

		SecurityAdvisoriesConfiguration securityAdvisoriesConfiguration =
			securityAdvisoriesHelper.getSecurityAdvisoriesConfiguration();

		if (!securityAdvisoriesConfiguration.enabled() ||
			!securityAdvisoriesConfiguration.loginAdvisoriesEnabled()) {

			return;
		}

		HttpSession httpSession = httpServletRequest.getSession();

		if (GetterUtil.getBoolean(
				httpSession.getAttribute(
					SecurityAdvisoriesWebKeys.SECURITY_ADVISORY_NOTIFIED))) {

			return;
		}

		httpSession.setAttribute(
			SecurityAdvisoriesWebKeys.SECURITY_ADVISORY_NOTIFIED, Boolean.TRUE);

		httpServletRequest.setAttribute(
			SecurityAdvisoriesWebKeys.SECURITY_ADVISORY_HELPER,
			securityAdvisoriesHelper);

		if (!securityAdvisoriesHelper.hasSecurityAdvisoriesRole(
				httpServletRequest)) {

			return;
		}

		super.include(httpServletRequest, httpServletResponse, key);
	}

	@Override
	public void register(DynamicIncludeRegistry dynamicIncludeRegistry) {
		dynamicIncludeRegistry.register("/html/common/themes/bottom.jsp#pre");
	}

	@Override
	protected String getJspPath() {
		return "/dynamic_include/bottom.jsp";
	}

	@Override
	protected Log getLog() {
		return _log;
	}

	@Override
	@Reference(
		target = "(osgi.web.symbolicname=jorgediazest.security.advisories)",
		unbind = "-"
	)
	protected void setServletContext(ServletContext servletContext) {
		super.setServletContext(servletContext);
	}

	@Reference
	protected SecurityAdvisoriesHelper securityAdvisoriesHelper;

	private static final Log _log = LogFactoryUtil.getLog(
		SecurityAdvisoriesBottomJSPDynamicInclude.class);

}