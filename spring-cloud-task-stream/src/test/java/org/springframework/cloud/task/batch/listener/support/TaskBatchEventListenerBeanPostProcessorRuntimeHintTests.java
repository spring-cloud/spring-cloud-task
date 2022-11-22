/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.task.batch.listener.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.cloud.task.batch.listener.support.TaskBatchEventListenerBeanPostProcessor.RuntimeHint;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * @author Henning Pöttker
 */
class TaskBatchEventListenerBeanPostProcessorRuntimeHintTests {

	private RuntimeHints hints;

	@BeforeEach
	void setUp() {
		this.hints = new RuntimeHints();
		new RuntimeHint().registerHints(this.hints, getClass().getClassLoader());
	}

	@Test
	void reflectionOnChunkProviderFieldIsAllowed() {
		var field = ReflectionUtils.findField(ChunkOrientedTasklet.class, "chunkProvider");
		assertThat(field).isNotNull();
		assertThat(reflection().onField(field)).accepts(this.hints);
	}

	@Test
	void reflectionOnChunkProcessorFieldIsAllowed() {
		var field = ReflectionUtils.findField(ChunkOrientedTasklet.class, "chunkProcessor");
		assertThat(field).isNotNull();
		assertThat(reflection().onField(field)).accepts(this.hints);
	}

}
