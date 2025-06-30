/*
 * Copyright 2016-2025 the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.batch.listener.support.TaskBatchEventListenerBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TaskBatchEventListenerBeanPostProcessorTests {

	@MockitoBean
	ItemProcessListener itemProcessListener;

	@MockitoBean
	StepExecutionListener stepExecutionListener;

	@MockitoBean
	ChunkListener chunkListener;

	@MockitoBean
	ItemReadListener itemReadListener;

	@MockitoBean
	ItemWriteListener itemWriteListener;

	@MockitoBean
	SkipListener skipListener;

	@MockitoBean
	private TaskletStep taskletStep;

	@MockitoBean
	private SimpleChunkProvider chunkProvider;

	@MockitoBean
	private SimpleChunkProcessor chunkProcessor;

	@Autowired
	private GenericApplicationContext context;

	@BeforeEach
	public void setupMock() {
		when(this.taskletStep.getTasklet())
				.thenReturn(new ChunkOrientedTasklet(this.chunkProvider, this.chunkProcessor));
		when(this.taskletStep.getName()).thenReturn("FOOOBAR");

		registerAlias(ItemProcessListener.class, BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER);
		registerAlias(StepExecutionListener.class, BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER);
		registerAlias(ChunkListener.class, BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER);
		registerAlias(ItemReadListener.class, BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER);
		registerAlias(ItemWriteListener.class, BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER);
		registerAlias(SkipListener.class, BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER);
	}

	@Test
	public void testPostProcessor() {
		TaskBatchEventListenerBeanPostProcessor postProcessor = this.context
			.getBean(TaskBatchEventListenerBeanPostProcessor.class);
		assertThat(postProcessor).isNotNull();
		TaskletStep updatedTaskletStep = (TaskletStep) postProcessor.postProcessBeforeInitialization(this.taskletStep,
				"FOO");
		assertThat(updatedTaskletStep).isEqualTo(this.taskletStep);
	}

	private void registerAlias(Class clazz, String name) {
		assertThat(this.context.getBeanNamesForType(clazz).length).isEqualTo(1);
		this.context.registerAlias(this.context.getBeanNamesForType(clazz)[0], name);

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	public static class TestConfiguration {

		@Bean
		public static TaskBatchEventListenerBeanPostProcessor taskBatchEventListenerBeanPostProcessor() {
			return new TaskBatchEventListenerBeanPostProcessor();
		}

	}

}
