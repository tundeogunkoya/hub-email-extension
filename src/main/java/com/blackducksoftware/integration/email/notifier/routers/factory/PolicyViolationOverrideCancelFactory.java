package com.blackducksoftware.integration.email.notifier.routers.factory;

import java.util.List;

import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.email.messaging.ItemRouter;
import com.blackducksoftware.integration.email.messaging.ItemRouterFactory;
import com.blackducksoftware.integration.email.messaging.RouterTaskData;
import com.blackducksoftware.integration.email.notifier.routers.PolicyViolationOverrideCancelRouter;
import com.blackducksoftware.integration.hub.notification.api.PolicyOverrideNotificationItem;

@Component
public class PolicyViolationOverrideCancelFactory extends ItemRouterFactory<List<PolicyOverrideNotificationItem>> {

	@Override
	public ItemRouter<List<PolicyOverrideNotificationItem>> createInstance(
			final RouterTaskData<List<PolicyOverrideNotificationItem>> data) {
		return new PolicyViolationOverrideCancelRouter(data);
	}
}
