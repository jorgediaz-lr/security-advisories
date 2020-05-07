<%--
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
--%>

<%@ include file="/dynamic_include/init.jsp" %>

<c:if test="<%= securityAdvisoriesHelper != null %>">
	<liferay-util:buffer
		var="securityAdvisoriesMessage"
	>
		<%@ include file="/security_advisories_message.jspf" %>
	</liferay-util:buffer>

	<c:if test="<%= Validator.isNotNull(securityAdvisoriesMessage) %>">

		<%
		String securityAdvisoriesMessageType = null;

		if (ListUtil.isNotEmpty(securityAdvisoriesHelper.getSev1Issues()) || ListUtil.isNotEmpty(securityAdvisoriesHelper.getSev2Issues())) {
			securityAdvisoriesMessageType = "danger";
		}
		else if (ListUtil.isNotEmpty(securityAdvisoriesHelper.getSev3Issues())) {
			securityAdvisoriesMessageType = "warning";
		}
		else {
			securityAdvisoriesMessageType = "info";
		}
		%>

		<c:choose>
			<c:when test='<%= securityAdvisoriesMessageType.equals("info") %>'>
				<aui:script use="liferay-notification">
					new Liferay.Notification(
						{
							closeable: true,
							delay: {
								hide: 5000,
								show: 0
							},
							duration: 500,
							message: '<%= HtmlUtil.escapeJS(securityAdvisoriesMessage) %>',
							render: true,
							title: '<liferay-ui:message key="<%= securityAdvisoriesMessageType %>" />',
							type: '<%= securityAdvisoriesMessageType %>'
						}
					);
				</aui:script>
			</c:when>
			<c:otherwise>
				<liferay-ui:alert
					icon="exclamation-full"
					message="<%= securityAdvisoriesMessage %>"
					targetNode="#controlMenuAlertsContainer"
					timeout="<%= 0 %>"
					type="<%= securityAdvisoriesMessageType %>"
				/>
			</c:otherwise>
		</c:choose>
	</c:if>
</c:if>