/*
 * Copyright 2017-2022 the original author or authors.
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

import java.util.Date;
import java.util.List;

import io.micrometer.core.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.task.repository.TaskExecution;

import static io.micrometer.core.tck.TestObservationRegistryAssert.then;

class ObservationTaskExecutionListenerTests {

	@Test
	void testSuccessfulObservation() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		ObservationTaskExecutionListener listener = new ObservationTaskExecutionListener(registry, "my-project");
		TaskExecution taskExecution = taskExecution();

		listener.onTaskStartup(taskExecution);
		listener.onTaskEnd(taskExecution);

		then(registry)
			.hasSingleObservationThat()
			.hasNameEqualTo("spring.cloud.task.execution")
			.hasContextualNameEqualTo("my-project");
	}

	@Test
	void testErrorObservation() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		ObservationTaskExecutionListener listener = new ObservationTaskExecutionListener(registry, "my-project");
		TaskExecution taskExecution = taskExecution();

		listener.onTaskStartup(taskExecution);
		listener.onTaskFailed(taskExecution, new RuntimeException("error"));

		then(registry)
			.hasSingleObservationThat()
			.hasNameEqualTo("spring.cloud.task.execution")
			.hasContextualNameEqualTo("my-project")
			.thenThrowable()
			.hasMessage("error");
	}

	private TaskExecution taskExecution() {
		return new TaskExecution(1L, 1, "task", new Date(), new Date(), "bye", List.of("arg"), "boom", "id");
	}
}
