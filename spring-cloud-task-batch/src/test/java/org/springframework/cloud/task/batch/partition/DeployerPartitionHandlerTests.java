/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.batch.partition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 */
public class DeployerPartitionHandlerTests {

	@Captor
	ArgumentCaptor<AppDeploymentRequest> appDeploymentRequestArgumentCaptor;

	@Mock
	private TaskLauncher taskLauncher;

	@Mock
	private JobExplorer jobExplorer;

	@Mock
	private Resource resource;

	@Mock
	private StepExecutionSplitter splitter;

	@Mock
	private JobRepository jobRepository;

	private Environment environment;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.environment = new MockEnvironment();
	}

	@Test
	public void testConstructorValidation() {
		validateConstructorValidation(null, null, null, null,
				"A taskLauncher is required");
		validateConstructorValidation(this.taskLauncher, null, null, null,
				"A jobExplorer is required");
		validateConstructorValidation(this.taskLauncher, this.jobExplorer, null, null,
				"A resource is required");
		validateConstructorValidation(this.taskLauncher, this.jobExplorer, this.resource,
				null, "A step name is required");

		new DeployerPartitionHandler(this.taskLauncher, this.jobExplorer, this.resource,
				"step-name");
	}

	@Test
	public void testNoPartitions() throws Exception {
		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		StepExecution stepExecution = new StepExecution("step1", new JobExecution(1L));

		when(this.splitter.split(stepExecution, 1)).thenReturn(new HashSet<>());

		Collection<StepExecution> results = handler.handle(this.splitter, stepExecution);

		verify(this.taskLauncher, never()).launch((AppDeploymentRequest) any());
		assertThat(results.isEmpty()).isTrue();
	}

	@Test
	public void testSinglePartition() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();

		assertThat(request.getResource()).isEqualTo(this.resource);
		assertThat(request.getDeploymentProperties().size()).isEqualTo(0);

		AppDefinition appDefinition = request.getDefinition();

		assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, "1")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID, "4")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, "step1"))).isTrue();

		assertThat(results.size()).isEqualTo(1);
		StepExecution resultStepExecution = results.iterator().next();
		assertThat(resultStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(resultStepExecution.getStepName()).isEqualTo("step1:partition1");
	}

	@Test
	public void testSinglePartitionAsEnvVars() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);
		handler.setDefaultArgsAsEnvironmentVars(true);

		TaskExecution taskExecution = new TaskExecution(55, null, null, null, null, null,
				new ArrayList<>(), null, null);
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();

		assertThat(request.getResource()).isEqualTo(this.resource);
		assertThat(request.getDeploymentProperties().size()).isEqualTo(0);

		AppDefinition appDefinition = request.getDefinition();

		assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
		assertThat(request.getCommandlineArguments().isEmpty()).isTrue();
		assertThat(request.getDefinition().getProperties()
				.get(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID))
						.isEqualTo("1");
		assertThat(request.getDefinition().getProperties()
				.get(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID))
						.isEqualTo("4");
		assertThat(request.getDefinition().getProperties()
				.get(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME))
						.isEqualTo("step1");
		assertThat(request.getDefinition().getProperties()
				.get(DeployerPartitionHandler.SPRING_CLOUD_TASK_NAME))
						.isEqualTo("partitionedJobTask_partitionedJob_step1:partition1");
		assertThat(request.getDefinition().getProperties()
				.get(DeployerPartitionHandler.SPRING_CLOUD_TASK_PARENT_EXECUTION_ID))
						.isEqualTo("55");

		assertThat(results.size()).isEqualTo(1);
		StepExecution resultStepExecution = results.iterator().next();
		assertThat(resultStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(resultStepExecution.getStepName()).isEqualTo("step1:partition1");
	}

	@Test
	public void testParentExecutionId() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		TaskExecution taskExecution = new TaskExecution(55, null, null, null, null, null,
				new ArrayList<>(), null, null);

		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		handler.handle(this.splitter, masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_PARENT_EXECUTION_ID, "55")))
						.isTrue();
	}

	@Test
	public void testThreePartitions() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart3 = getStepExecutionStart(jobExecution, 6L);
		StepExecution workerStepExecutionFinish3 = getStepExecutionFinish(
				workerStepExecutionStart3, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();

		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		stepExecutions.add(workerStepExecutionStart3);

		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);
		when(this.jobExplorer.getStepExecution(1L, 6L))
				.thenReturn(workerStepExecutionFinish3);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher, times(3))
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		List<AppDeploymentRequest> allValues = this.appDeploymentRequestArgumentCaptor
				.getAllValues();

		validateAppDeploymentRequests(allValues, 3);

		validateStepExecutionResults(results);
	}

	@Test
	public void testThreePartitionsTwoWorkers() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart3 = getStepExecutionStart(jobExecution, 6L);
		StepExecution workerStepExecutionFinish3 = getStepExecutionFinish(
				workerStepExecutionStart3, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);
		handler.setMaxWorkers(2);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();

		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		stepExecutions.add(workerStepExecutionStart3);

		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);
		when(this.jobExplorer.getStepExecution(1L, 6L))
				.thenReturn(workerStepExecutionFinish3);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher, times(3))
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		List<AppDeploymentRequest> allValues = this.appDeploymentRequestArgumentCaptor
				.getAllValues();

		validateAppDeploymentRequests(allValues, 3);

		validateStepExecutionResults(results);
	}

	@Test
	public void testFailedWorker() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.FAILED);

		StepExecution workerStepExecutionStart3 = getStepExecutionStart(jobExecution, 6L);
		StepExecution workerStepExecutionFinish3 = getStepExecutionFinish(
				workerStepExecutionStart3, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);
		handler.setMaxWorkers(2);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();

		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		stepExecutions.add(workerStepExecutionStart3);

		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);
		when(this.jobExplorer.getStepExecution(1L, 6L))
				.thenReturn(workerStepExecutionFinish3);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher, times(3))
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		List<AppDeploymentRequest> allValues = this.appDeploymentRequestArgumentCaptor
				.getAllValues();

		validateAppDeploymentRequests(allValues, 3);

		Iterator<StepExecution> resultsIterator = results.iterator();
		Set<String> names = new HashSet<>(results.size());

		while (resultsIterator.hasNext()) {
			StepExecution curResult = resultsIterator.next();

			if (curResult.getStepName().equals("step1:partition2")) {
				assertThat(curResult.getStatus()).isEqualTo(BatchStatus.FAILED);
			}
			else {
				assertThat(curResult.getStatus()).isEqualTo(BatchStatus.COMPLETED);
			}

			assertThat(!names.contains(curResult.getStepName())).isTrue();
			names.add(curResult.getStepName());
		}
	}

	@Test
	public void testPassingEnvironmentProperties() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");

		Map<String, String> environmentParameters = new HashMap<>(2);
		environmentParameters.put("foo", "bar");
		environmentParameters.put("baz", "qux");

		SimpleEnvironmentVariablesProvider environmentVariablesProvider = new SimpleEnvironmentVariablesProvider(
				this.environment);
		environmentVariablesProvider.setEnvironmentProperties(environmentParameters);
		handler.setEnvironmentVariablesProvider(environmentVariablesProvider);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();

		assertThat(request.getResource()).isEqualTo(this.resource);
		assertThat(request.getDefinition().getProperties().size()).isEqualTo(2);
		assertThat(request.getDefinition().getProperties().get("foo")).isEqualTo("bar");
		assertThat(request.getDefinition().getProperties().get("baz")).isEqualTo("qux");

		AppDefinition appDefinition = request.getDefinition();

		assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, "1")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID, "4")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, "step1"))).isTrue();

		assertThat(results.size()).isEqualTo(1);
		StepExecution resultStepExecution = results.iterator().next();
		assertThat(resultStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(resultStepExecution.getStepName()).isEqualTo("step1:partition1");
	}

	@Test
	public void testOverridingEnvironmentProperties() throws Exception {

		((MockEnvironment) this.environment).setProperty("foo", "zoo");
		((MockEnvironment) this.environment).setProperty("task", "batch");

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		Map<String, String> environmentParameters = new HashMap<>(2);
		environmentParameters.put("foo", "bar");
		environmentParameters.put("baz", "qux");

		SimpleEnvironmentVariablesProvider environmentVariablesProvider = new SimpleEnvironmentVariablesProvider(
				this.environment);
		environmentVariablesProvider.setEnvironmentProperties(environmentParameters);
		handler.setEnvironmentVariablesProvider(environmentVariablesProvider);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();

		assertThat(request.getResource()).isEqualTo(this.resource);
		assertThat(request.getDefinition().getProperties().size()).isEqualTo(3);
		assertThat(request.getDefinition().getProperties().get("foo")).isEqualTo("bar");
		assertThat(request.getDefinition().getProperties().get("baz")).isEqualTo("qux");
		assertThat(request.getDefinition().getProperties().get("task"))
				.isEqualTo("batch");

		AppDefinition appDefinition = request.getDefinition();

		assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, "1")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID, "4")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, "step1"))).isTrue();

		assertThat(results.size()).isEqualTo(1);
		StepExecution resultStepExecution = results.iterator().next();
		assertThat(resultStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(resultStepExecution.getStepName()).isEqualTo("step1:partition1");
	}

	@Test
	public void testPollInterval() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		handler.setPollInterval(20000L);
		handler.setMaxWorkers(1);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		Date startTime = new Date();
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);
		Date endTime = new Date();
		verify(this.taskLauncher, times(2))
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		List<AppDeploymentRequest> allRequests = this.appDeploymentRequestArgumentCaptor
				.getAllValues();

		validateAppDeploymentRequests(allRequests, 2);

		validateStepExecutionResults(results);

		assertThat(endTime.getTime() - startTime.getTime() >= 19999)
				.as("Time difference was too small: "
						+ (endTime.getTime() - startTime.getTime()))
				.isTrue();
	}

	@Test(expected = TimeoutException.class)
	public void testTimeout() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		handler.setPollInterval(20000L);
		handler.setMaxWorkers(1);
		handler.setTimeout(1000L);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		handler.handle(this.splitter, masterStepExecution);
	}

	@Test
	public void testGridSize() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart1 = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish1 = getStepExecutionFinish(
				workerStepExecutionStart1, BatchStatus.COMPLETED);

		StepExecution workerStepExecutionStart2 = getStepExecutionStart(jobExecution, 5L);
		StepExecution workerStepExecutionFinish2 = getStepExecutionFinish(
				workerStepExecutionStart2, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		handler.setGridSize(2);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart1);
		stepExecutions.add(workerStepExecutionStart2);
		when(this.splitter.split(masterStepExecution, 2)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish1);
		when(this.jobExplorer.getStepExecution(1L, 5L))
				.thenReturn(workerStepExecutionFinish2);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);

		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher, times(2))
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		List<AppDeploymentRequest> allRequests = this.appDeploymentRequestArgumentCaptor
				.getAllValues();

		validateAppDeploymentRequests(allRequests, 2);

		validateStepExecutionResults(results);
	}

	@Test
	public void testDeployerProperties() throws Exception {

		StepExecution masterStepExecution = createMasterStepExecution();
		JobExecution jobExecution = masterStepExecution.getJobExecution();

		StepExecution workerStepExecutionStart = getStepExecutionStart(jobExecution, 4L);
		StepExecution workerStepExecutionFinish = getStepExecutionFinish(
				workerStepExecutionStart, BatchStatus.COMPLETED);

		DeployerPartitionHandler handler = new DeployerPartitionHandler(this.taskLauncher,
				this.jobExplorer, this.resource, "step1");
		handler.setEnvironment(this.environment);

		Map<String, String> deploymentProperties = new HashMap<>(2);
		deploymentProperties.put("foo", "bar");
		deploymentProperties.put("baz", "qux");

		handler.setDeploymentProperties(deploymentProperties);

		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("partitionedJobTask");

		Set<StepExecution> stepExecutions = new HashSet<>();
		stepExecutions.add(workerStepExecutionStart);
		when(this.splitter.split(masterStepExecution, 1)).thenReturn(stepExecutions);

		when(this.jobExplorer.getStepExecution(1L, 4L))
				.thenReturn(workerStepExecutionFinish);

		handler.afterPropertiesSet();

		handler.beforeTask(taskExecution);
		Collection<StepExecution> results = handler.handle(this.splitter,
				masterStepExecution);

		verify(this.taskLauncher)
				.launch(this.appDeploymentRequestArgumentCaptor.capture());

		AppDeploymentRequest request = this.appDeploymentRequestArgumentCaptor.getValue();

		assertThat(request.getResource()).isEqualTo(this.resource);
		assertThat(request.getDeploymentProperties().size()).isEqualTo(2);
		assertThat(request.getDeploymentProperties().get("foo")).isEqualTo("bar");
		assertThat(request.getDeploymentProperties().get("baz")).isEqualTo("qux");

		AppDefinition appDefinition = request.getDefinition();

		assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, "1")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID, "4")))
						.isTrue();
		assertThat(request.getCommandlineArguments().contains(formatArgs(
				DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, "step1"))).isTrue();

		assertThat(results.size()).isEqualTo(1);
		StepExecution resultStepExecution = results.iterator().next();
		assertThat(resultStepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(resultStepExecution.getStepName()).isEqualTo("step1:partition1");
	}

	private String formatArgs(String key, String value) {
		return String.format("--%s=%s", key, value);
	}

	private StepExecution getStepExecutionFinish(StepExecution stepExecutionStart,
			BatchStatus status) {
		StepExecution workerStepExecutionFinish = new StepExecution(
				stepExecutionStart.getStepName(), stepExecutionStart.getJobExecution());
		workerStepExecutionFinish.setId(stepExecutionStart.getId());
		workerStepExecutionFinish.setStatus(status);
		return workerStepExecutionFinish;
	}

	private StepExecution getStepExecutionStart(JobExecution jobExecution, long id) {
		StepExecution workerStepExecutionStart = new StepExecution(
				"step1:partition" + (id - 3), jobExecution);
		workerStepExecutionStart.setId(id);
		return workerStepExecutionStart;
	}

	private StepExecution createMasterStepExecution() {

		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setJobInstance(new JobInstance(2L, "partitionedJob"));

		StepExecution masterStepExecution = new StepExecution("masterStep", jobExecution);
		masterStepExecution.setId(3L);

		return masterStepExecution;
	}

	private void validateStepExecutionResults(Collection<StepExecution> results) {
		Iterator<StepExecution> resultsIterator = results.iterator();
		Set<String> names = new HashSet<>(results.size());

		while (resultsIterator.hasNext()) {
			StepExecution curResult = resultsIterator.next();

			assertThat(curResult.getStatus()).isEqualTo(BatchStatus.COMPLETED);

			assertThat(!names.contains(curResult.getStepName())).isTrue();
			names.add(curResult.getStepName());
		}
	}

	private void validateAppDeploymentRequests(List<AppDeploymentRequest> allRequests,
			int numberOfPartitions) {
		Collections.sort(allRequests, new Comparator<AppDeploymentRequest>() {
			@Override
			public int compare(AppDeploymentRequest o1, AppDeploymentRequest o2) {
				List<String> commandlineArguments = o1.getCommandlineArguments();

				String o1Command = "";
				for (String commandlineArgument : commandlineArguments) {
					if (commandlineArgument.contains(
							DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)) {
						o1Command = commandlineArgument;
						break;
					}
				}

				commandlineArguments = o2.getCommandlineArguments();

				String o2Command = "";
				for (String commandlineArgument : commandlineArguments) {
					if (commandlineArgument.contains(
							DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID)) {
						o2Command = commandlineArgument;
						break;
					}
				}

				return o1Command.compareTo(o2Command);
			}
		});

		for (int i = 4; i < (numberOfPartitions + 4); i++) {
			AppDeploymentRequest request = allRequests.get(i - 4);
			assertThat(request.getResource()).isEqualTo(this.resource);
			assertThat(request.getDeploymentProperties().size()).isEqualTo(0);

			AppDefinition appDefinition = request.getDefinition();
			assertThat(appDefinition.getName()).isEqualTo("partitionedJobTask");
			assertThat(request.getCommandlineArguments().contains(formatArgs(
					DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID, "1")))
							.isTrue();
			assertThat(request.getCommandlineArguments()
					.contains(formatArgs(
							DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID,
							String.valueOf(i)))).isTrue();
			assertThat(request.getCommandlineArguments().contains(formatArgs(
					DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, "step1")))
							.isTrue();
		}
	}

	private void validateConstructorValidation(TaskLauncher taskLauncher,
			JobExplorer jobExplorer, Resource resource, String stepName,
			String expectedMessage) {
		try {
			new DeployerPartitionHandler(taskLauncher, jobExplorer, resource, stepName);
		}
		catch (IllegalArgumentException iae) {
			assertThat(iae.getMessage()).isEqualTo(expectedMessage);
		}
	}

}
