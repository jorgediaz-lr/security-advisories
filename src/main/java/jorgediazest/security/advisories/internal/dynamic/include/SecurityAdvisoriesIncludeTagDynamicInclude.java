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
import com.liferay.portal.kernel.servlet.taglib.TagDynamicInclude;
import com.liferay.portal.kernel.util.PortletKeys;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jorgediazest.security.advisories.SecurityAdvisoriesHelper;
import jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration;
import jorgediazest.security.advisories.constants.SecurityAdvisoriesWebKeys;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Díaz
 */
@Component(immediate = true, service = TagDynamicInclude.class)
public class SecurityAdvisoriesIncludeTagDynamicInclude
	implements TagDynamicInclude {

	@Override
	public void include(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String tagClassName,
			String tagDynamicId, String tagPoint)
		throws IOException {

		SecurityAdvisoriesConfiguration securityAdvisoriesConfiguration =
			securityAdvisoriesHelper.getSecurityAdvisoriesConfiguration();

		if (!securityAdvisoriesConfiguration.enabled()) {
			return;
		}

		if (!securityAdvisoriesHelper.hasSecurityAdvisoriesRole(
				httpServletRequest)) {

			return;
		}

		httpServletRequest.setAttribute(
			SecurityAdvisoriesWebKeys.SECURITY_ADVISORY_HELPER,
			securityAdvisoriesHelper);

		RequestDispatcher requestDispatcher =
			this._servletContext.getRequestDispatcher(getJspPath());

		try {
			requestDispatcher.include(httpServletRequest, httpServletResponse);
		}
		catch (ServletException servletException) {
			_log.error(
				"Unable to include JSP " + getJspPath(), servletException);

			throw new IOException(
				"Unable to include JSP " + getJspPath(), servletException);
		}
	}

	@Override
	public void register(TagDynamicIncludeRegistry tagDynamicIncludeRegistry) {
		tagDynamicIncludeRegistry.register(
			"com.liferay.taglib.util.IncludeTag",
			PortletKeys.SERVER_ADMIN + "/resources.jsp", "doEndTag#before");
	}

	protected String getJspPath() {
		return "/dynamic_include/resources.jsp";
	}

	@Reference(
		target = "(osgi.web.symbolicname=jorgediazest.security.advisories)",
		unbind = "-"
	)
	protected void setServletContext(ServletContext servletContext) {
		_servletContext = servletContext;
	}

	@Reference
	protected SecurityAdvisoriesHelper securityAdvisoriesHelper;

	private static final Log _log = LogFactoryUtil.getLog(
		SecurityAdvisoriesIncludeTagDynamicInclude.class);

	private ServletContext _servletContext;

}