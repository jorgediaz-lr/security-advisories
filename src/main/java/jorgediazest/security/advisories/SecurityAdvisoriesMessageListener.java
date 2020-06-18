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

import com.liferay.portal.kernel.messaging.BaseMessageListener;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.module.framework.ModuleServiceLifecycle;
import com.liferay.portal.kernel.scheduler.SchedulerEngineHelper;
import com.liferay.portal.kernel.scheduler.SchedulerEntry;
import com.liferay.portal.kernel.scheduler.SchedulerEntryImpl;
import com.liferay.portal.kernel.scheduler.TimeUnit;
import com.liferay.portal.kernel.scheduler.Trigger;
import com.liferay.portal.kernel.scheduler.TriggerFactory;

import java.util.Date;
import java.util.Map;

import jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jorge Díaz
 */
@Component(
	configurationPid = "jorgediazest.security.advisories.configuration.SecurityAdvisoriesConfiguration",
	configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true,
	service = {}
)
public class SecurityAdvisoriesMessageListener extends BaseMessageListener {

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		deactivate();

		SecurityAdvisoriesConfiguration securityAdvisoriesConfiguration =
			securityAdvisoriesHelper.getSecurityAdvisoriesConfiguration();

		Class<?> clazz = getClass();

		String className = clazz.getName();

		Trigger trigger = triggerFactory.createTrigger(
			className, className, new Date(), null,
			securityAdvisoriesConfiguration.refreshDataInterval(),
			TimeUnit.MINUTE);

		SchedulerEntry schedulerEntry = new SchedulerEntryImpl(
			className, trigger);

		schedulerEngineHelper.register(
			this, schedulerEntry, DestinationNames.SCHEDULER_DISPATCH);

		registeredScheduler = true;
	}

	@Deactivate
	protected void deactivate() {
		if (registeredScheduler) {
			schedulerEngineHelper.unregister(this);

			registeredScheduler = false;
		}
	}

	@Override
	protected void doReceive(Message message) throws Exception {
		securityAdvisoriesHelper.initialize();

		securityAdvisoriesHelper.writeLogTraces();
	}

	@Reference(target = ModuleServiceLifecycle.PORTAL_INITIALIZED)
	protected ModuleServiceLifecycle moduleServiceLifecycle;

	protected boolean registeredScheduler = false;

	@Reference
	protected SchedulerEngineHelper schedulerEngineHelper;

	@Reference
	protected SecurityAdvisoriesHelper securityAdvisoriesHelper;

	@Reference
	protected TriggerFactory triggerFactory;

}