/*
 * Copyright 2022-present the original author or authors.
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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Supports the launching of partitions.
 *
 * @author Glenn Renfro
 */
public class TaskLauncherHandler implements Runnable {

	private CommandLineArgsProvider commandLineArgsProvider;

	private TaskRepository taskRepository;

	private boolean defaultArgsAsEnvironmentVars;

	private String stepName;

	private TaskExecution taskExecution;

	private EnvironmentVariablesProvider environmentVariablesProvider;

	private Resource resource;

	private Map<String, String> deploymentProperties;

	private org.springframework.cloud.deployer.spi.task.TaskLauncher taskLauncher;

	private String applicationName;

	private StepExecution workerStepExecution;

	private Log logger = LogFactory.getLog(TaskLauncherHandler.class);

	/**
	 * @param commandLineArgsProvider The {@link CommandLineArgsProvider} that provides
	 * command line arguments passed to each partition's execution.
	 * @param taskRepository The {@link TaskRepository} task repository for launching the
	 * partition.
	 * @param defaultArgsAsEnvironmentVars - If set to true, the default args that are
	 * used internally by Spring Cloud Task and Spring Batch are passed as environment
	 * variables instead of command line arguments.
	 * @param stepName The name of the step.
	 * @param taskExecution The {@link TaskExecution} to be associated with the partition.
	 * @param environmentVariablesProvider {@link EnvironmentVariablesProvider} that
	 * provides the environmennt variables.
	 * @param resource The {@link Resource} to be launched.
	 * @param deploymentProperties The {@link Map} containing the deployment properties
	 * for the partition.
	 * @param taskLauncher
	 * {@link org.springframework.cloud.deployer.spi.task.TaskLauncher} that is used to
	 * launch the partition.
	 * @param applicationName The name to be associated with task.
	 * @param workerStepExecution The {@link StepExecution} for the paritition.
	 */
	public TaskLauncherHandler(CommandLineArgsProvider commandLineArgsProvider, TaskRepository taskRepository,
			boolean defaultArgsAsEnvironmentVars, String stepName, TaskExecution taskExecution,
			EnvironmentVariablesProvider environmentVariablesProvider, Resource resource,
			Map<String, String> deploymentProperties,
			org.springframework.cloud.deployer.spi.task.TaskLauncher taskLauncher, String applicationName,
			StepExecution workerStepExecution) {
		this.commandLineArgsProvider = commandLineArgsProvider;
		this.taskRepository = taskRepository;
		this.defaultArgsAsEnvironmentVars = defaultArgsAsEnvironmentVars;
		this.stepName = stepName;
		this.taskExecution = taskExecution;
		this.environmentVariablesProvider = environmentVariablesProvider;
		this.resource = resource;
		this.deploymentProperties = deploymentProperties;
		this.taskLauncher = taskLauncher;
		this.applicationName = applicationName;
		this.workerStepExecution = workerStepExecution;
	}

	/**
	 * @param commandLineArgsProvider The {@link CommandLineArgsProvider} that provides
	 * command line arguments passed to each partition's execution.
	 * @param taskRepository The {@link TaskRepository} task repository for launching the
	 * partition.
	 * @param defaultArgsAsEnvironmentVars - If set to true, the default args that are
	 * used internally by Spring Cloud Task and Spring Batch are passed as environment
	 * variables instead of command line arguments.
	 * @param stepName The name of the step.
	 * @param taskExecution The {@link TaskExecution} to be associated with the partition.
	 * @param environmentVariablesProvider {@link EnvironmentVariablesProvider} that
	 * provides the environmennt variables.
	 * @param resource The {@link Resource} to be launched.
	 * @param deploymentProperties The {@link Map} containing the deployment properties
	 * for the partition.
	 * @param taskLauncher
	 * {@link org.springframework.cloud.deployer.spi.task.TaskLauncher} that is used to
	 * launch the partition.
	 * @param applicationName The name to be associated with task.
	 */
	public TaskLauncherHandler(CommandLineArgsProvider commandLineArgsProvider, TaskRepository taskRepository,
			boolean defaultArgsAsEnvironmentVars, String stepName, TaskExecution taskExecution,
			EnvironmentVariablesProvider environmentVariablesProvider, Resource resource,
			Map<String, String> deploymentProperties,
			org.springframework.cloud.deployer.spi.task.TaskLauncher taskLauncher, String applicationName) {
		this.commandLineArgsProvider = commandLineArgsProvider;
		this.taskRepository = taskRepository;
		this.defaultArgsAsEnvironmentVars = defaultArgsAsEnvironmentVars;
		this.stepName = stepName;
		this.taskExecution = taskExecution;
		this.environmentVariablesProvider = environmentVariablesProvider;
		this.resource = resource;
		this.deploymentProperties = deploymentProperties;
		this.taskLauncher = taskLauncher;
		this.applicationName = applicationName;
	}

	@Override
	public void run() {
		launchWorker(this.workerStepExecution);
	}

	/**
	 * Launches the partition for the StepExecution.
	 * @param workerStepExecution The {@link StepExecution}
	 */
	public void launchWorker(StepExecution workerStepExecution) {
		List<String> arguments = new ArrayList<>();

		ExecutionContext copyContext = new ExecutionContext(workerStepExecution.getExecutionContext());

		arguments.addAll(this.commandLineArgsProvider.getCommandLineArgs(copyContext));

		TaskExecution partitionTaskExecution = null;

		if (this.taskRepository != null) {
			partitionTaskExecution = this.taskRepository.createTaskExecution();
		}
		else {
			logger.warn("TaskRepository was not set so external execution id will not be recorded.");
		}

		if (!this.defaultArgsAsEnvironmentVars) {
			arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID,
					String.valueOf(workerStepExecution.getJobExecution().getId())));
			arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID,
					String.valueOf(workerStepExecution.getId())));
			arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, this.stepName));
			arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_NAME,
					String.format("%s_%s_%s", this.taskExecution.getTaskName(),
							workerStepExecution.getJobExecution().getJobInstance().getJobName(),
							workerStepExecution.getStepName())));
			arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_PARENT_EXECUTION_ID,
					String.valueOf(this.taskExecution.getExecutionId())));

			if (partitionTaskExecution != null) {
				arguments.add(formatArgument(DeployerPartitionHandler.SPRING_CLOUD_TASK_EXECUTION_ID,
						String.valueOf(partitionTaskExecution.getExecutionId())));
			}
		}

		copyContext = new ExecutionContext(workerStepExecution.getExecutionContext());

		Map<String, String> environmentVariables = this.environmentVariablesProvider
			.getEnvironmentVariables(copyContext);

		if (this.defaultArgsAsEnvironmentVars) {
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID,
					String.valueOf(workerStepExecution.getJobExecution().getId()));
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID,
					String.valueOf(workerStepExecution.getId()));
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME, this.stepName);
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_NAME,
					String.format("%s_%s_%s", this.taskExecution.getTaskName(),
							workerStepExecution.getJobExecution().getJobInstance().getJobName(),
							workerStepExecution.getStepName()));
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_PARENT_EXECUTION_ID,
					String.valueOf(this.taskExecution.getExecutionId()));
			environmentVariables.put(DeployerPartitionHandler.SPRING_CLOUD_TASK_EXECUTION_ID,
					String.valueOf(partitionTaskExecution.getExecutionId()));
		}

		AppDefinition definition = new AppDefinition(resolveApplicationName(), environmentVariables);

		AppDeploymentRequest request = new AppDeploymentRequest(definition, this.resource, this.deploymentProperties,
				arguments);

		if (logger.isDebugEnabled()) {
			logger.debug("Requesting the launch of the following application: " + request);
		}
		String externalExecutionId = this.taskLauncher.launch(request);

		if (this.taskRepository != null) {
			this.taskRepository.updateExternalExecutionId(partitionTaskExecution.getExecutionId(), externalExecutionId);
		}
	}

	private String formatArgument(String key, String value) {
		return String.format("--%s=%s", key, value);
	}

	private String resolveApplicationName() {
		if (StringUtils.hasText(this.applicationName)) {
			return this.applicationName;
		}
		else {
			return this.taskExecution.getTaskName();
		}
	}

}
