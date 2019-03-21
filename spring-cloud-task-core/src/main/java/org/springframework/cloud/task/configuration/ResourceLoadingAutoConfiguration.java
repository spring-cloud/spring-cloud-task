/*
 * Copyright 2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Autoconfiguration of a file or Maven based {@link ResourceLoader}.
 *
 * @author Michael Minella
 * @since 1.0.1
 */
@Configuration
public class ResourceLoadingAutoConfiguration {

	@Configuration
	@ConditionalOnClass(MavenResourceLoader.class)
	public static class MavenResourceLoadingAutoConfiguration {

		@Bean
		public MavenResourceLoader mavenResourceLoader(MavenProperties mavenProperties) {
			return new MavenResourceLoader(mavenProperties);
		}

		@Bean
		@ConditionalOnMissingBean
		public DelegatingResourceLoader delegatingResourceLoader(MavenResourceLoader mavenResourceLoader) {
			Map<String, ResourceLoader> loaders = new HashMap<>(1);
			loaders.put("maven", mavenResourceLoader);

			return new DelegatingResourceLoader(loaders);
		}

		@Bean
		public MavenProperties mavenProperties() {
			return new MavenConfigurationProperties();
		}

		@ConfigurationProperties(prefix = "maven")
		public static class MavenConfigurationProperties extends MavenProperties {}
	}

	@Configuration
	@ConditionalOnMissingClass("org.springframework.cloud.deployer.resource.maven.MavenResourceLoader")
	public static class LocalResourceLoadingAutoConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public DelegatingResourceLoader delegatingResourceLoader() {
			return new DelegatingResourceLoader();
		}
	}
}
