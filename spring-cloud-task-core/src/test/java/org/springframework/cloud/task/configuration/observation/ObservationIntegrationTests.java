/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.cloud.task.configuration.observation;

import java.util.List;
import java.util.stream.Collectors;

import brave.sampler.Sampler;
import brave.test.TestSpanHandler;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveFinishedSpan;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.simple.SpansAssert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfigureObservability
@SpringBootTest(classes = ObservationIntegrationTests.Config.class)
class ObservationIntegrationTests {

	@Autowired
	TestSpanHandler testSpanHandler;

	@Autowired
	MeterRegistry meterRegistry;

	@Test
	void testSuccessfulObservation() {
		List<FinishedSpan> finishedSpans = finishedSpans();

		SpansAssert.then(finishedSpans)
			.thenASpanWithNameEqualTo("my-command-line-runner")
			.hasTag("spring.cloud.task.runner.bean-name", "myCommandLineRunner")
			.backToSpans()
			.thenASpanWithNameEqualTo("my-application-runner")
			.hasTag("spring.cloud.task.runner.bean-name", "myApplicationRunner");
		MeterRegistryAssert.then(this.meterRegistry)
			.hasTimerWithNameAndTags("spring.cloud.task.runner",
					KeyValues.of("spring.cloud.task.runner.bean-name", "myCommandLineRunner"))
			.hasTimerWithNameAndTags("spring.cloud.task.runner",
					KeyValues.of("spring.cloud.task.runner.bean-name", "myApplicationRunner"));
	}

	private List<FinishedSpan> finishedSpans() {
		return this.testSpanHandler.spans().stream().map(BraveFinishedSpan::fromBrave).collect(Collectors.toList());
	}

	@Configuration
	@EnableTask
	@ImportAutoConfiguration({ SimpleTaskAutoConfiguration.class, ObservationAutoConfiguration.class,
			ObservationTaskAutoConfiguration.class, BraveAutoConfiguration.class,
			MicrometerTracingAutoConfiguration.class, MetricsAutoConfiguration.class,
			CompositeMeterRegistryAutoConfiguration.class, ZipkinAutoConfiguration.class })
	static class Config {

		private static final Logger log = LoggerFactory.getLogger(Config.class);

		@Bean
		TestSpanHandler testSpanHandler() {
			return new TestSpanHandler();
		}

		@Bean
		Sampler sampler() {
			return Sampler.ALWAYS_SAMPLE;
		}

		@Bean
		CommandLineRunner myCommandLineRunner(Tracer tracer) {
			return args -> log.info("<TRACE:{}> Hello from command line runner",
					tracer.currentSpan().context().traceId());
		}

		@Bean
		ApplicationRunner myApplicationRunner(Tracer tracer) {
			return args -> log.info("<TRACE:{}> Hello from application runner",
					tracer.currentSpan().context().traceId());
		}

	}

}
