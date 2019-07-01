/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.task.metrics.fork.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Micrometer common tags for Cloud Foundry deployment properties. Based on the CF
 * application environment variables:
 * https://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html
 *
 * Tags are set only if the "cloud" Spring profile is set. The "cloud" profile is
 * activated automatically when an application is deployed in CF:
 * https://docs.cloudfoundry.org/buildpacks/java/configuring-service-connections/spring-service-bindings.html#cloud-profiles
 *
 * Use the spring.cloud.task.metrics.cf.tags.enabled=false property to disable inserting
 * those tags.
 *
 * @author Christian Tzolov
 */
@Configuration
@Profile("cloud")
@ConditionalOnProperty(name = "spring.cloud.task.metrics.cf.tags.enabled",
		havingValue = "true", matchIfMissing = true)
public class CloudFoundryMicrometerTagsConfiguration {

	@Value("${vcap.application.org_name:default}")
	private String organizationName;

	@Value("${vcap.application.space_id:unknown}")
	private String spaceId;

	@Value("${vcap.application.space_name:unknown}")
	private String spaceName;

	@Value("${vcap.application.application_name:unknown}")
	private String applicationName;

	@Value("${vcap.application.application_id:unknown}")
	private String applicationId;

	@Value("${vcap.application.application_version:unknown}")
	private String applicationVersion;

	@Value("${vcap.application.instance_index:0}")
	private String instanceIndex;

	@Bean
	public MeterRegistryCustomizer<MeterRegistry> cloudFoundryMetricsCommonTags() {
		return registry -> registry.config().commonTags("cf.org.name", organizationName)
				.commonTags("cf.space.id", spaceId).commonTags("cf.space.name", spaceName)
				.commonTags("cf.app.id", applicationId)
				.commonTags("cf.app.name", applicationName)
				.commonTags("cf.app.version", applicationVersion)
				.commonTags("cf.instance.index", instanceIndex);
	}

}
