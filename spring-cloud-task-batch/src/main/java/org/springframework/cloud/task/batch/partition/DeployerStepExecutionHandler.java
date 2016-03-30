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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.integration.partition.BeanFactoryStepLocator;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * <p>A {@link CommandLineRunner} used to execute a {@link Step}.  No result is provided
 * directly to the associated {@link DeployerPartitionHandler} as it will obtain the step
 * results directly from the shared job repository.</p>
 *
 * <p>The {@link StepExecution} is rehydrated based on the environment variables provided.
 * Specifically, the following variables are required:</p>
 * <ul>
 *     <li>{@link DeployerPartitionHandler#SPRING_CLOUD_TASK_JOB_EXECUTION_ID}: The id of
 *     the JobExecution.</li>
 *     <li>{@link DeployerPartitionHandler#SPRING_CLOUD_TASK_STEP_EXECUTION_ID}: The id of
 *     the StepExecution.</li>
 *     <li>{@link DeployerPartitionHandler#SPRING_CLOUD_TASK_STEP_NAME}: The id of the
 *     bean definition for the Step to execute.  The id must be found within the provided
 *     {@link BeanFactory}</li>
 * </ul>
 *
 * @author Michael Minella
 */
public class DeployerStepExecutionHandler implements CommandLineRunner {

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private Log logger = LogFactory.getLog(DeployerStepExecutionHandler.class);

	@Autowired
	private Environment environment;

	private StepLocator stepLocator;

	public DeployerStepExecutionHandler(BeanFactory beanFactory, JobExplorer jobExplorer, JobRepository jobRepository) {
		Assert.notNull(beanFactory, "A beanFactory is required");
		Assert.notNull(jobExplorer, "A jobExplorer is required");
		Assert.notNull(jobRepository, "A jobRepository is required");

		this.stepLocator = new BeanFactoryStepLocator();
		((BeanFactoryStepLocator) this.stepLocator).setBeanFactory(beanFactory);

		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
	}

	@Override
	public void run(String... args) throws Exception {

		validateRequest();

		Long jobExecutionId = Long.parseLong(environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID));
		Long stepExecutionId = Long.parseLong(environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID));
		StepExecution stepExecution = jobExplorer.getStepExecution(jobExecutionId, stepExecutionId);

		if (stepExecution == null) {
			throw new NoSuchStepException(String.format("No StepExecution could be located for step execution id %s within job execution %s", stepExecutionId, jobExecutionId));
		}

		String stepName = environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME);
		Step step = stepLocator.getStep(stepName);

		try {
			logger.debug(String.format("Executing step %s with step execution id %s and job execution id %s", stepExecution.getStepName(), stepExecutionId, jobExecutionId));

			step.execute(stepExecution);
		}
		catch (JobInterruptedException e) {
			stepExecution.setStatus(BatchStatus.STOPPED);
			jobRepository.update(stepExecution);
		}
		catch (Throwable e) {
			stepExecution.addFailureException(e);
			stepExecution.setStatus(BatchStatus.FAILED);
			jobRepository.update(stepExecution);
		}
	}

	private void validateRequest() {
		Assert.isTrue(environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_JOB_EXECUTION_ID), "A job execution id is required");
		Assert.isTrue(environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_EXECUTION_ID), "A step execution id is required");
		Assert.isTrue(environment.containsProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME), "A step name is required");

		Assert.isTrue(this.stepLocator.getStepNames().contains(environment.getProperty(DeployerPartitionHandler.SPRING_CLOUD_TASK_STEP_NAME)), "The step requested cannot be found in the provided BeanFactory");
	}
}
