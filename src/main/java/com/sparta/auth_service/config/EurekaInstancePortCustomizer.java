package com.sparta.auth_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * server.port=0(랜덤) 기동 시 Eureka instance-id를 실제 포트로 맞춤.
 * Gateway lb:// 라우팅 stale port 방지 — auth-service:실제포트 형식.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EurekaInstancePortCustomizer implements ApplicationListener<WebServerInitializedEvent> {

	private static final Logger log = LoggerFactory.getLogger(EurekaInstancePortCustomizer.class);

	private final EurekaInstanceConfigBean instanceConfig;
	private final Environment environment;

	public EurekaInstancePortCustomizer(EurekaInstanceConfigBean instanceConfig, Environment environment) {
		this.instanceConfig = instanceConfig;
		this.environment = environment;
	}

	@Override
	public void onApplicationEvent(WebServerInitializedEvent event) {
		int port = event.getWebServer().getPort();
		String appName = environment.getProperty("spring.application.name", "application");
		String instanceId = appName + ":" + port;
		instanceConfig.setInstanceId(instanceId);
		log.info("Auth service started on port {} (Eureka instance-id: {})", port, instanceId);
	}

}
