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

package jorgediazest.taglib;

import com.liferay.portal.kernel.servlet.taglib.TagDynamicIdFactory;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Díaz
 */
@Component(
	immediate = true,
	property = "tagClassName=com.liferay.taglib.util.IncludeTag",
	service = TagDynamicIdFactory.class
)
public class IncludeTagDynamicIdFactory implements TagDynamicIdFactory {

	@Override
	public String getTagDynamicId(
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, Object tag) {

		String portletId = _portal.getPortletId(httpServletRequest);

		if (Validator.isNull(portletId)) {
			return null;
		}

		String page;

		try {
			Class<?> tagClass = tag.getClass();

			Method getPageMethod = tagClass.getDeclaredMethod("getPage");

			getPageMethod.setAccessible(true);

			page = (String)getPageMethod.invoke(tag);
		}
		catch (Exception exception) {
			return null;
		}

		if (Validator.isNull(page)) {
			return null;
		}

		return portletId.concat(page);
	}

	@Reference
	private Portal _portal;

}