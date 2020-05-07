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

		<liferay-ui:panel-container
			extended="<%= true %>"
			id="securityAdvisoriesPanelContainer"
			persistState="<%= true %>"
		>
			<div class="panel panel-default server-admin-tabs" id="securityAdvisoriesInformationPanel">
				<div class="panel-body">
					<div class="alert alert-<%= securityAdvisoriesMessageType %>" style="margin-bottom: 0px;"><%= securityAdvisoriesMessage %></div>
				</div>
			</div>
		</liferay-ui:panel-container>
	</c:if>
</c:if>