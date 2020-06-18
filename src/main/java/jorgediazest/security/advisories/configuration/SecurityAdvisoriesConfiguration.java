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

package jorgediazest.security.advisories.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Jorge Díaz
 */
@ExtendedObjectClassDefinition(
	category = "other", scope = ExtendedObjectClassDefinition.Scope.SYSTEM
)
@Meta.OCD(
	id = "jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration",
	localization = "content/Language",
	name = "security-advisories-configuration-name"
)
public interface SecurityAdvisoriesConfiguration {

	@Meta.AD(
		deflt = "true", description = "enabled-help", name = "enabled",
		required = false
	)
	public boolean enabled();

	@Meta.AD(
		deflt = "true", description = "login-advisories-enabled-help",
		name = "login-advisories-enabled", required = false
	)
	public boolean loginAdvisoriesEnabled();

	@Meta.AD(
		deflt = "Administrator", description = "roles-help", name = "roles",
		required = false
	)
	public String[] roles();

	@Meta.AD(
		deflt = "4320", description = "refresh-data-interval-help",
		name = "refresh-data-interval", required = false
	)
	public int refreshDataInterval();

	@Meta.AD(
		deflt = "", description = "issues-to-ignore-help",
		name = "issues-to-ignore", required = false
	)
	public String issuesToIgnore();

}