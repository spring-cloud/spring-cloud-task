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
package org.springframework.cloud.task.batch.partition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.poller.DirectPoller;
import org.springframework.batch.poller.Poller;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * <p>A {@link PartitionHandler} implementation that delegates to a {@link TaskLauncher} for
 * each of the workers.  The id of the worker's StepExecution is passed as an environment
 * variable to the worker.  The worker, bootstrapped by the
 * {@link DeployerStepExecutionHandler}, looks up the StepExecution in the JobRepository
 * and executes it.  This PartitionHandler polls the JobRepository for the results.</p>
 *
 * <p>If the job fails, the partitions will be re-executed per normal batch rules (steps that
 * are complete should do nothing, failed steps should restart based on their
 * configurations).</p>
 *
 * <p>This PartitionHandler and all of the worker processes must share the same JobRepository
 * data store (aka point the same database).</p>
 *
 * @author Michael Minella
 */
public class DeployerPartitionHandler implements PartitionHandler, EnvironmentAware, InitializingBean {

	public static final String SPRING_CLOUD_TASK_JOB_EXECUTION_ID =
			"spring.cloud.task.job-execution-id";

	public static final String SPRING_CLOUD_TASK_STEP_EXECUTION_ID =
			"spring.cloud.task.step-execution-id";

	public static final String SPRING_CLOUD_TASK_STEP_NAME =
			"spring.cloud.task.step-name";

	private int maxWorkers = -1;

	private int gridSize = 1;

	private int currentWorkers = 0;

	private TaskLauncher taskLauncher;

	private JobExplorer jobExplorer;

	private TaskExecution taskExecution;

	private Resource resource;

	private String stepName;

	private Log logger = LogFactory.getLog(DeployerPartitionHandler.class);

	private long pollInterval = 10000;

	private long timeout = -1;

	private Environment environment;

	private Map<String, String> deploymentProperties;

	private EnvironmentVariablesProvider environmentVariablesProvider;

	public DeployerPartitionHandler(TaskLauncher taskLauncher,
			JobExplorer jobExplorer,
			Resource resource,
			String stepName) {
		Assert.notNull(taskLauncher, "A taskLauncher is required");
		Assert.notNull(jobExplorer, "A jobExplorer is required");
		Assert.notNull(resource, "A resource is required");
		Assert.hasText(stepName, "A step name is required");

		this.taskLauncher = taskLauncher;
		this.jobExplorer = jobExplorer;
		this.resource = resource;
		this.stepName = stepName;
	}

	public void setEnvironmentVariablesProvider(EnvironmentVariablesProvider environmentVariablesProvider) {
		this.environmentVariablesProvider = environmentVariablesProvider;
	}

	/**
	 * The maximum number of workers to be executing at once.
	 *
	 * @param maxWorkers number of workers.  Defaults to -1 (unlimited)
	 */
	public void setMaxWorkers(int maxWorkers) {
		Assert.isTrue(maxWorkers != 0, "maxWorkers cannot be 0");
		this.maxWorkers = maxWorkers;
	}

	/**
	 * Approximate size of the pool of worker JVMs available.  May be used by the
	 * {@link StepExecutionSplitter} to determine how many partitions to create (at the
	 * discretion of the {@link org.springframework.batch.core.partition.support.Partitioner}).
	 *
	 * @param gridSize size of grid.  Defaults to 1
	 */
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	/**
	 * The interval to check the job repository for completed steps.
	 *
	 * @param pollInterval interval.  Defaults to 10 seconds
	 */
	public void setPollInterval(long pollInterval) {
		this.pollInterval = pollInterval;
	}

	/**
	 * Timeout for the master step.  This is a timeout for all workers to complete.
	 *
	 * @param timeout timeout.  Defaults to none (-1).
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Map of deployment properties to be used by the {@link TaskLauncher}
	 *
	 * @param deploymentProperties properites to be used by the {@link TaskLauncher}
	 */
	public void setDeploymentProperties(Map<String, String> deploymentProperties) {
		this.deploymentProperties = deploymentProperties;
	}

	@BeforeTask
	public void beforeTask(TaskExecution taskExecution) {
		this.taskExecution = taskExecution;
	}

	@Override
	public Collection<StepExecution> handle(StepExecutionSplitter stepSplitter,
			StepExecution stepExecution) throws Exception {

		final Set<StepExecution> tempCandidates =
				stepSplitter.split(stepExecution, this.gridSize);

		// Following two lines due to https://jira.spring.io/browse/BATCH-2490
		final Set<StepExecution> candidates = new HashSet<>(tempCandidates.size());
		candidates.addAll(tempCandidates);

		int partitions = candidates.size();

		logger.debug(String.format("%s partitions were returned", partitions));

		final Set<StepExecution> executed = new HashSet<>(candidates.size());

		if (CollectionUtils.isEmpty(candidates)) {
			return null;
		}

		launchWorkers(candidates, executed);

		candidates.removeAll(executed);

		return pollReplies(stepExecution, executed, candidates, partitions);
	}

	private void launchWorkers(Set<StepExecution> candidates, Set<StepExecution> executed) {
		for (StepExecution execution : candidates) {
			if (this.currentWorkers < this.maxWorkers || this.maxWorkers < 0) {
				launchWorker(execution);
				this.currentWorkers++;

				executed.add(execution);
			}
		}
	}

	private void launchWorker(StepExecution workerStepExecution) {
		List<String> arguments = new ArrayList<>();
		arguments.addAll(this.taskExecution.getArguments());
		arguments.add(formatArgument(SPRING_CLOUD_TASK_JOB_EXECUTION_ID,
				String.valueOf(workerStepExecution.getJobExecution().getId())));
		arguments.add(formatArgument(SPRING_CLOUD_TASK_STEP_EXECUTION_ID,
				String.valueOf(workerStepExecution.getId())));
		arguments.add(formatArgument(SPRING_CLOUD_TASK_STEP_NAME, this.stepName));

		ExecutionContext copyContext = new ExecutionContext(workerStepExecution.getExecutionContext());

		AppDefinition definition =
				new AppDefinition(String.format("%s:%s:%s",
						taskExecution.getTaskName(),
						workerStepExecution.getJobExecution().getJobInstance().getJobName(),
						workerStepExecution.getStepName()),
						this.environmentVariablesProvider.getEnvironmentVariables(copyContext));

		AppDeploymentRequest request =
				new AppDeploymentRequest(definition,
						this.resource,
						this.deploymentProperties,
						arguments);

		taskLauncher.launch(request);
	}

	private String formatArgument(String key, String value) {
		return String.format("--%s=%s", key, value);
	}

	private Collection<StepExecution> pollReplies(final StepExecution masterStepExecution,
			final Set<StepExecution> executed,
			final Set<StepExecution> candidates,
			final int size) throws Exception {

		final Collection<StepExecution> result = new ArrayList<>(executed.size());

		Callable<Collection<StepExecution>> callback = new Callable<Collection<StepExecution>>() {
			@Override
			public Collection<StepExecution> call() throws Exception {
				Set<StepExecution> newExecuted = new HashSet<>();

				for (StepExecution curStepExecution : executed) {
					if (!result.contains(curStepExecution)) {
						StepExecution partitionStepExecution =
								jobExplorer.getStepExecution(masterStepExecution.getJobExecutionId(), curStepExecution.getId());

						if (isComplete(partitionStepExecution.getStatus())) {
							result.add(partitionStepExecution);
							currentWorkers--;

							if (!candidates.isEmpty()) {

								launchWorkers(candidates, newExecuted);
								candidates.removeAll(newExecuted);
							}
						}
					}
				}

				executed.addAll(newExecuted);

				if (result.size() == size) {
					return result;
				}
				else {
					return null;
				}
			}
		};

		Poller<Collection<StepExecution>> poller = new DirectPoller<>(this.pollInterval);
		Future<Collection<StepExecution>> resultsFuture = poller.poll(callback);

		if (timeout >= 0) {
			return resultsFuture.get(timeout, TimeUnit.MILLISECONDS);
		}
		else {
			return resultsFuture.get();
		}
	}

	private boolean isComplete(BatchStatus status) {
		return status.equals(BatchStatus.COMPLETED) || status.isGreaterThan(BatchStatus.STARTED);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(this.environmentVariablesProvider == null) {
			this.environmentVariablesProvider =
					new SimpleEnvironmentVariablesProvider(this.environment);

		}
	}
}
