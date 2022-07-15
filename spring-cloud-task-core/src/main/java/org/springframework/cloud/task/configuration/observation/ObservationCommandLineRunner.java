/*
 * Copyright 2018-2022 the original author or authors.
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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.CommandLineRunner;

/**
 * Observed representation of a {@link CommandLineRunner}.
 *
 * @author Marcin Grzejszczak
 */
class ObservationCommandLineRunner implements CommandLineRunner {

	private static final DefaultTaskObservationConvention INSTANCE = new DefaultTaskObservationConvention();

	private final BeanFactory beanFactory;

	private final CommandLineRunner delegate;

	private final String beanName;

	private ObservationRegistry registry;

	private TaskObservationConvention taskObservationConvention;

	ObservationCommandLineRunner(BeanFactory beanFactory, CommandLineRunner delegate, String beanName) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
		this.beanName = beanName;
	}

	@Override
	public void run(String... args) throws Exception {
		TaskObservationContext context = new TaskObservationContext(this.beanName);
		Observation observation = TaskDocumentedObservation.TASK_RUNNER_OBSERVATION.observation(this.taskObservationConvention, INSTANCE, context, registry())
			.contextualName(this.beanName);
		try (Observation.Scope scope = observation.start().openScope()) {
			this.delegate.run(args);
		}
		catch (Exception error) {
			observation.error(error);
			throw error;
		}
		finally {
			observation.stop();
		}
	}

	private ObservationRegistry registry() {
		if (this.registry == null) {
			this.registry = this.beanFactory.getBean(ObservationRegistry.class);
		}
		return this.registry;
	}
}
