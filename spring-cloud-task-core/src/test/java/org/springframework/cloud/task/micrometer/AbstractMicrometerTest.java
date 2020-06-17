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

package org.springframework.cloud.task.micrometer;

import java.io.IOException;
import java.nio.charset.Charset;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.pivotal.cfenv.test.CfEnvTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Soby Chacko
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { AbstractMicrometerTest.AutoConfigurationApplication.class })
@DirtiesContext
public class AbstractMicrometerTest {

	@Autowired
	protected SimpleMeterRegistry simpleMeterRegistry;

	@Autowired
	protected ConfigurableApplicationContext context;

	protected Meter meter;

	@BeforeEach
	public void before() {
		Metrics.globalRegistry.getMeters().forEach(Metrics.globalRegistry::remove);
		assertThat(simpleMeterRegistry).isNotNull();
		meter = simpleMeterRegistry.find("spring.integration.handlers").meter();
		assertThat(meter).isNotNull().withFailMessage(
				"The spring.integration.handlers meter must be present in SpringBoot apps!");
	}

	@AfterEach
	public void after() {
		Metrics.globalRegistry.getMeters().forEach(Metrics.globalRegistry::remove);
	}

	@BeforeAll
	public static void setup() throws IOException {
		String serviceJson = StreamUtils.copyToString(new DefaultResourceLoader()
				.getResource("classpath:/micrometer/pcf-scs-info.json").getInputStream(),
				Charset.forName("UTF-8"));
		CfEnvTestUtils.mockVcapServicesFromString(serviceJson);
	}

	@SpringBootApplication
	public static class AutoConfigurationApplication {

		public static void main(String[] args) {
			SpringApplication.run(AutoConfigurationApplication.class, args);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleMeterRegistry simpleMeterRegistry(SimpleConfig config, Clock clock) {
			return new SimpleMeterRegistry(config, clock);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleConfig simpleConfig() {
			return key -> null;
		}

	}

}
