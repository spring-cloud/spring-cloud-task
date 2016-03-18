/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.task.batch.partition;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 */
public class DeployerStepExecutionHandlerTests {

	@Mock
	private JobExplorer jobExplorer;

	@Mock
	private JobRepository jobRepository;

	@Mock
	private Environment environment;

	@Mock
	private ListableBeanFactory beanFactory;

	@Mock
	private Step step;

	@Captor
	private ArgumentCaptor<StepExecution> stepExecutionArgumentCaptor;

	private DeployerStepExecutionHandler handler;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		this.handler = new DeployerStepExecutionHandler(this.beanFactory, this.jobExplorer, this.jobRepository);

		ReflectionTestUtils.setField(this.handler, "environment", this.environment);
	}

	@Test
	public void testConstructorValidation() {
		validateConstructorValidation(null, null, null, "A beanFactory is required");
		validateConstructorValidation(this.beanFactory, null, null, "A jobExplorer is required");
		validateConstructorValidation(this.beanFactory, this.jobExplorer, null, "A jobRepository is required");

		new DeployerStepExecutionHandler(this.beanFactory, this.jobExplorer, this.jobRepository);
	}

	@Test
	public void testValidationOfRequestValuesExist() throws Exception {
		validateEnvironmentConfiguration("A job execution id is required", new String[0]);
		validateEnvironmentConfiguration("A step execution id is required", new String[] {DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID});
		validateEnvironmentConfiguration("A step name is required", new String[] {DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID});
	}

	@Test
	public void testValidationOfRequestStepFound() throws Exception {
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn(true);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("foo");
		when(this.beanFactory.getBeanNamesForType(Step.class)).thenReturn(new String[] {"bar", "baz"});

		try {
			this.handler.run();
		}
		catch (IllegalArgumentException iae) {
			assertEquals("The step requested cannot be found in the provided BeanFactory", iae.getMessage());
		}
	}

	@Test
	public void testMissingStepExecution() throws Exception {
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn(true);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("foo");
		when(this.beanFactory.getBeanNamesForType(Step.class)).thenReturn(new String[] {"foo", "bar", "baz"});
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn("2");
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn("1");

		try {
			this.handler.run();
		}
		catch (NoSuchStepException nsse) {
			assertEquals("No StepExecution could be located for step execution id 2 within job execution 1", nsse.getMessage());
		}
	}

	@Test
	public void testRunSuccessful() throws Exception {
		StepExecution workerStep = new StepExecution("workerStep", new JobExecution(1L), 2L);

		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn(true);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBeanNamesForType(Step.class)).thenReturn(new String[] {"workerStep", "foo", "bar"});
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn("2");
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn("1");
		when(this.jobExplorer.getStepExecution(1L, 2L)).thenReturn(workerStep);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBean("workerStep", Step.class)).thenReturn(this.step);

		handler.run();

		verify(this.step).execute(workerStep);
		verifyZeroInteractions(this.jobRepository);
	}

	@Test
	public void testJobInterruptedException() throws Exception {
		StepExecution workerStep = new StepExecution("workerStep", new JobExecution(1L), 2L);

		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn(true);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBeanNamesForType(Step.class)).thenReturn(new String[] {"workerStep", "foo", "bar"});
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn("2");
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn("1");
		when(this.jobExplorer.getStepExecution(1L, 2L)).thenReturn(workerStep);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBean("workerStep", Step.class)).thenReturn(this.step);
		doThrow(new JobInterruptedException("expected")).when(this.step).execute(workerStep);

		handler.run();

		verify(this.jobRepository).update(this.stepExecutionArgumentCaptor.capture());

		assertEquals(BatchStatus.STOPPED, this.stepExecutionArgumentCaptor.getValue().getStatus());
	}

	@Test
	public void testRuntimeException() throws Exception {
		StepExecution workerStep = new StepExecution("workerStep", new JobExecution(1L), 2L);

		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn(true);
		when(this.environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn(true);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBeanNamesForType(Step.class)).thenReturn(new String[] {"workerStep", "foo", "bar"});
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)).thenReturn("2");
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID)).thenReturn("1");
		when(this.jobExplorer.getStepExecution(1L, 2L)).thenReturn(workerStep);
		when(this.environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)).thenReturn("workerStep");
		when(this.beanFactory.getBean("workerStep", Step.class)).thenReturn(this.step);
		doThrow(new RuntimeException("expected")).when(this.step).execute(workerStep);

		handler.run();

		verify(this.jobRepository).update(this.stepExecutionArgumentCaptor.capture());

		assertEquals(BatchStatus.FAILED, this.stepExecutionArgumentCaptor.getValue().getStatus());
	}

	private void validateEnvironmentConfiguration(String errorMessage, String[] properties) throws Exception {

		for (String property : properties) {
			when(this.environment.containsProperty(property)).thenReturn(true);
		}

		try {
			this.handler.run();
		}
		catch (IllegalArgumentException iae) {
			assertEquals(errorMessage, iae.getMessage());
		}
	}


	private void validateConstructorValidation(BeanFactory beanFactory, JobExplorer jobExplorer, JobRepository jobRepository, String message) {
		try {
			new DeployerStepExecutionHandler(beanFactory, jobExplorer, jobRepository);
		}
		catch (IllegalArgumentException iae) {
			assertEquals(message, iae.getMessage());
		}
	}
}
