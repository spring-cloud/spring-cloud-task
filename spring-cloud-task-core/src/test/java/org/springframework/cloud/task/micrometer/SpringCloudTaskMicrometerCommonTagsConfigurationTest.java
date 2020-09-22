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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@Nested
public class SpringCloudTaskMicrometerCommonTagsConfigurationTest {

	public static class TestDefaultTagValues extends AbstractMicrometerTest {

		@Test
		public void testDefaultTagValues() {
			assertThat(meter.getId().getTag("task.name")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("task.execution.id")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("task.parent.execution.id")).isEqualTo("unknown");
			assertThat(meter.getId().getTag("task.external.execution.id")).isEqualTo("unknown");
		}

	}

	@TestPropertySource(properties = { "spring.cloud.task.name=myTask", "spring.cloud.task.executionid=123",
			"spring.cloud.task.parent-execution-id=999", "spring.cloud.task.external-execution-id=696" })
	public static class TestPresetTagValues extends AbstractMicrometerTest {

		@Test
		public void testPresetTagValues() {
			assertThat(meter.getId().getTag("task.name")).isEqualTo("myTask");
			assertThat(meter.getId().getTag("task.execution.id")).isEqualTo("123");
			assertThat(meter.getId().getTag("task.parent.execution.id")).isEqualTo("999");
			assertThat(meter.getId().getTag("task.external.execution.id")).isEqualTo("696");
		}

	}

	@TestPropertySource(properties = { "spring.cloud.task.metrics.common.tags.enabled=false" })
	public static class TestDisabledTagValues extends AbstractMicrometerTest {

		@Test
		public void testDefaultTagValues() {
			assertThat(meter.getId().getTag("task.name")).isNull();
			assertThat(meter.getId().getTag("task.execution.id")).isNull();
			assertThat(meter.getId().getTag("task.parent.execution.id")).isNull();
			assertThat(meter.getId().getTag("task.external.execution.id")).isNull();
		}

	}

}
