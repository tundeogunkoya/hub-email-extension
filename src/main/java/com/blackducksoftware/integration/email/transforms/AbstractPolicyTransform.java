package com.blackducksoftware.integration.email.transforms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.email.model.EmailContentItem;
import com.blackducksoftware.integration.hub.api.component.BomComponentVersionPolicyStatus;
import com.blackducksoftware.integration.hub.api.component.ComponentVersionStatus;
import com.blackducksoftware.integration.hub.api.notification.NotificationItem;
import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.api.version.ReleaseItem;
import com.blackducksoftware.integration.hub.exception.NotificationServiceException;
import com.blackducksoftware.integration.hub.notification.NotificationService;

public abstract class AbstractPolicyTransform extends AbstractTransform {
	private final Logger logger = LoggerFactory.getLogger(AbstractPolicyTransform.class);

	public AbstractPolicyTransform(final NotificationService notificationService) {
		super(notificationService);
	}

	public void handleNotification(final String projectName, final List<ComponentVersionStatus> componentVersionList,
			final ReleaseItem releaseItem, final NotificationItem item, final List<EmailContentItem> templateData) {
		for (final ComponentVersionStatus componentVersion : componentVersionList) {
			try {
				final String componentVersionName = getNotificationService()
						.getComponentVersion(componentVersion.getComponentVersionLink()).getVersionName();
				final String policyStatusUrl = componentVersion.getBomComponentVersionPolicyStatusLink();
				final BomComponentVersionPolicyStatus bomComponentVersionPolicyStatus = getNotificationService()
						.getPolicyStatus(policyStatusUrl);
				final List<String> ruleList = getRules(
						bomComponentVersionPolicyStatus.getLinks(BomComponentVersionPolicyStatus.POLICY_RULE_URL));
				if (ruleList == null || ruleList.isEmpty()) {
					logger.warn(
							"No configured policy violations matching this notification found; skipping this notification");
				} else {
					final List<String> ruleNameList = new ArrayList<>();
					for (final String ruleUrl : ruleList) {
						final PolicyRule rule = getNotificationService().getPolicyRule(ruleUrl);
						logger.debug("Policy Rule : " + rule);
						ruleNameList.add(rule.getName());
					}
					createContents(projectName, releaseItem.getVersionName(), componentVersion.getComponentName(),
							componentVersionName, StringUtils.join(ruleNameList, ", "), item, templateData);
				}

			} catch (final NotificationServiceException e) {
				logger.error("Error processing ComponentVersion: " + componentVersion, e);
			}
		}
	}

	private List<String> getRules(final List<String> rulesViolated) throws NotificationServiceException {
		if (rulesViolated == null || rulesViolated.isEmpty()) {
			logger.warn("No violated Rules provided.");
			return null;
		}
		final List<String> matchingRules = new ArrayList<>();
		for (final String ruleViolated : rulesViolated) {
			logger.debug("Violated rule (original): " + ruleViolated);
			final String fixedRuleUrl = fixRuleUrl(ruleViolated);
			logger.debug("Checking configured rules to monitor for fixed url: " + fixedRuleUrl);
			matchingRules.add(fixedRuleUrl);
		}
		return matchingRules;
	}

	/**
	 * In Hub versions prior to 3.2, the rule URLs contained in notifications
	 * are internal. To match the configured rule URLs, the "internal" segment
	 * of the URL from the notification must be removed. This is the workaround
	 * recommended by Rob P. In Hub 3.2 on, these URLs will exclude the
	 * "internal" segment.
	 *
	 * @param origRuleUrl
	 * @return
	 */
	private String fixRuleUrl(final String origRuleUrl) {
		String fixedRuleUrl = origRuleUrl;
		if (origRuleUrl.contains("/internal/")) {
			fixedRuleUrl = origRuleUrl.replace("/internal/", "/");
			logger.debug("Adjusted rule URL from " + origRuleUrl + " to " + fixedRuleUrl);
		}
		return fixedRuleUrl;
	}

	public abstract void createContents(String projectName, String projectVersion, String componentName,
			String componentVersion, String policyName, NotificationItem item, List<EmailContentItem> templateData);
}
