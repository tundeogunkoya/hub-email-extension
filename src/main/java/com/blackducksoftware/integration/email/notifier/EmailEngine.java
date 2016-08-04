package com.blackducksoftware.integration.email.notifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.email.ExtensionLogger;
import com.blackducksoftware.integration.email.model.CustomerProperties;
import com.blackducksoftware.integration.email.notifier.routers.factory.AbstractEmailFactory;
import com.blackducksoftware.integration.email.notifier.routers.factory.PolicyViolationFactory;
import com.blackducksoftware.integration.email.notifier.routers.factory.PolicyViolationOverrideCancelFactory;
import com.blackducksoftware.integration.email.notifier.routers.factory.PolicyViolationOverrideFactory;
import com.blackducksoftware.integration.email.notifier.routers.factory.VulnerabilityFactory;
import com.blackducksoftware.integration.email.service.EmailMessagingService;
import com.blackducksoftware.integration.email.service.properties.HubServerBeanConfiguration;
import com.blackducksoftware.integration.hub.HubIntRestService;
import com.blackducksoftware.integration.hub.exception.BDRestException;
import com.blackducksoftware.integration.hub.exception.EncryptionException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.item.HubItemsService;
import com.blackducksoftware.integration.hub.notification.NotificationService;
import com.blackducksoftware.integration.hub.notification.api.NotificationItem;
import com.blackducksoftware.integration.hub.notification.api.PolicyOverrideNotificationItem;
import com.blackducksoftware.integration.hub.notification.api.RuleViolationNotificationItem;
import com.blackducksoftware.integration.hub.notification.api.VulnerabilityNotificationItem;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class EmailEngine {
	private final Logger logger = LoggerFactory.getLogger(EmailEngine.class);

	public final Gson gson;
	public final Configuration configuration;
	public final DateFormat notificationDateFormat;
	public final Date applicationStartDate;
	public final ExecutorService executorService;

	public final List<AbstractEmailFactory> routerFactoryList;
	public final EmailMessagingService emailMessagingService;
	public final NotificationDispatcher notificationDispatcher;
	public final HubServerConfig hubServerConfig;
	public final Properties appProperties;
	public final CustomerProperties customerProperties;
	public final NotificationService notificationService;

	public EmailEngine() throws IOException, EncryptionException, URISyntaxException, BDRestException {
		gson = new Gson();
		appProperties = createAppProperties();
		customerProperties = createCustomerProperties();
		configuration = createFreemarkerConfig();
		hubServerConfig = createHubConfig();

		notificationDateFormat = createNotificationDateFormat();
		applicationStartDate = createApplicationStartDate();
		executorService = createExecutorService();
		emailMessagingService = createEmailMessagingService();
		notificationService = createNotificationService();
		routerFactoryList = createRouterFactoryList();
		notificationDispatcher = createDispatcher();

		notificationDispatcher.init();
		notificationDispatcher.attachRouters(routerFactoryList);
		notificationDispatcher.start();
	}

	private Properties createAppProperties() throws IOException {
		final Properties appProperties = new Properties();
		final String customerPropertiesPath = System.getProperty("customer.properties");
		final File customerPropertiesFile = new File(customerPropertiesPath);
		try (FileInputStream fileInputStream = new FileInputStream(customerPropertiesFile)) {
			appProperties.load(fileInputStream);
		}

		return appProperties;
	}

	private Configuration createFreemarkerConfig() throws IOException {
		final Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
		cfg.setDirectoryForTemplateLoading(new File(customerProperties.getEmailTemplateDirectory()));
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
		cfg.setLogTemplateExceptions(false);

		return cfg;
	}

	private DateFormat createNotificationDateFormat() {
		final DateFormat dateFormat = new SimpleDateFormat(RestConnection.JSON_DATE_FORMAT);
		dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
		return dateFormat;
	}

	private Date createApplicationStartDate() {
		return new Date();
	}

	private ExecutorService createExecutorService() {
		final ThreadFactory threadFactory = Executors.defaultThreadFactory();
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);
	}

	private CustomerProperties createCustomerProperties() {
		return new CustomerProperties(appProperties);
	}

	private EmailMessagingService createEmailMessagingService() {
		return new EmailMessagingService(customerProperties, configuration);
	}

	private List<AbstractEmailFactory> createRouterFactoryList() {
		final List<AbstractEmailFactory> factoryList = new Vector<>();
		factoryList.add(new PolicyViolationFactory(emailMessagingService, customerProperties, notificationService));
		factoryList.add(new PolicyViolationOverrideCancelFactory(emailMessagingService, customerProperties,
				notificationService));
		factoryList.add(
				new PolicyViolationOverrideFactory(emailMessagingService, customerProperties, notificationService));
		factoryList.add(new VulnerabilityFactory(emailMessagingService, customerProperties, notificationService));

		return factoryList;
	}

	private HubServerConfig createHubConfig() {
		final HubServerBeanConfiguration serverBeanConfig = new HubServerBeanConfiguration(customerProperties);

		return serverBeanConfig.build();
	}

	private NotificationDispatcher createDispatcher() {
		return new NotificationDispatcher(hubServerConfig, applicationStartDate, customerProperties, executorService,
				notificationService);
	}

	private NotificationService createNotificationService()
			throws EncryptionException, URISyntaxException, BDRestException {
		if (hubServerConfig == null) {
			return new NotificationService(null, null, null, new ExtensionLogger(logger));
		} else {
			final RestConnection restConnection = initRestConnection();
			final HubItemsService<NotificationItem> hubItemsService = initHubItemsService(restConnection);
			final HubIntRestService hub = new HubIntRestService(restConnection);
			return new NotificationService(restConnection, hub, hubItemsService, new ExtensionLogger(logger));
		}
	}

	private RestConnection initRestConnection() throws EncryptionException, URISyntaxException, BDRestException {
		final RestConnection restConnection = new RestConnection(hubServerConfig.getHubUrl().toString());

		restConnection.setCookies(hubServerConfig.getGlobalCredentials().getUsername(),
				hubServerConfig.getGlobalCredentials().getDecryptedPassword());
		restConnection.setProxyProperties(hubServerConfig.getProxyInfo());

		restConnection.setTimeout(hubServerConfig.getTimeout());
		return restConnection;
	}

	private HubItemsService<NotificationItem> initHubItemsService(final RestConnection restConnection) {
		final TypeToken<NotificationItem> typeToken = new TypeToken<NotificationItem>() {
		};
		final Map<String, Class<? extends NotificationItem>> typeToSubclassMap = new HashMap<>();
		typeToSubclassMap.put("VULNERABILITY", VulnerabilityNotificationItem.class);
		typeToSubclassMap.put("RULE_VIOLATION", RuleViolationNotificationItem.class);
		typeToSubclassMap.put("POLICY_OVERRIDE", PolicyOverrideNotificationItem.class);
		final HubItemsService<NotificationItem> hubItemsService = new HubItemsService<>(restConnection,
				NotificationItem.class, typeToken, typeToSubclassMap);
		return hubItemsService;
	}

}