/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.cloud.task.batch.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersIncrementer;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent;
import org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner;
import org.springframework.cloud.task.batch.configuration.TaskBatchProperties;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationRunner} to {@link JobOperator launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default and throws an exception upon the first job
 * that returns an {@link BatchStatus} of FAILED if a {@link TaskExecutor} in the
 * {@link JobOperator} is not specified. If a {@link TaskExecutor} is specified in the
 * {@link JobOperator} then all Jobs are launched and an exception is thrown if one or
 * more of the jobs has an {@link BatchStatus} of FAILED. TaskJobLauncherApplicationRunner
 * can also be used to launch a specific job by providing a jobName. The
 * TaskJobLauncherApplicationRunner takes the place of the
 * {@link JobLauncherApplicationRunner} when it is in use.
 *
 * @author Glenn Renfro
 * @since 2.3.0
 */
public class TaskJobLauncherApplicationRunner extends JobLauncherApplicationRunner {

	private static final Log logger = LogFactory.getLog(TaskJobLauncherApplicationRunner.class);

	private final JobOperator taskJobOperator;

	private final JobRepository taskJobRepository;

	private final List<JobExecution> jobExecutionList = new ArrayList<>();

	private @Nullable ApplicationEventPublisher taskApplicationEventPublisher;

	private final TaskBatchProperties taskBatchProperties;

	/**
	 * Create a new {@link TaskJobLauncherApplicationRunner}.
	 * @param jobOperator to launch jobs
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 * @param taskBatchProperties the properties used to configure the
	 * taskBatchProperties.
	 */
	public TaskJobLauncherApplicationRunner(JobOperator jobOperator, JobRepository jobRepository,
			TaskBatchProperties taskBatchProperties) {
		super(jobOperator);
		this.taskJobOperator = jobOperator;
		this.taskJobRepository = jobRepository;
		this.taskBatchProperties = taskBatchProperties;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		super.setApplicationEventPublisher(publisher);
		this.taskApplicationEventPublisher = publisher;
	}

	@Override
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		Properties properties = StringUtils.splitArrayElementsIntoProperties(args, "=");
		if (properties == null) {
			properties = new Properties();
		}
		launchJobFromProperties(properties);
		monitorJobExecutions();
	}

	@Override
	protected void execute(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, InvalidJobParametersException {
		String jobName = job.getName();
		JobParameters parameters = jobParameters;
		boolean jobInstanceExists = this.taskJobRepository.getJobInstance(job.getName(), jobParameters) != null;
		if (jobInstanceExists) {
			JobExecution lastJobExecution = this.taskJobRepository.getLastJobExecution(jobName, jobParameters);
			if (lastJobExecution != null && isStoppedOrFailed(lastJobExecution) && job.isRestartable()) {
				// Retry a failed or stopped execution with previous parameters
				JobParameters previousParameters = lastJobExecution.getJobParameters();
				/*
				 * remove Non-identifying parameters from the previous execution's
				 * parameters since there is no way to remove them programmatically. If
				 * they are required (or need to be modified) on a restart, they need to
				 * be (re)specified.
				 */
				JobParameters previousIdentifyingParameters = removeNonIdentifying(previousParameters);
				// merge additional parameters with previous ones (overriding those with
				// the same key)
				parameters = merge(previousIdentifyingParameters, jobParameters);
			}
		}
		else {
			JobParametersIncrementer incrementer = job.getJobParametersIncrementer();
			if (incrementer != null) {
				JobParameters nextParameters = getNextJobParameters(job, new HashSet<>(jobParameters.parameters()),
						this.taskJobRepository);
				parameters = merge(nextParameters, jobParameters);
			}
		}
		JobExecution execution = this.taskJobOperator.start(job, parameters);
		if (this.taskApplicationEventPublisher != null) {
			this.taskApplicationEventPublisher.publishEvent(new JobExecutionEvent(execution));
		}
		this.jobExecutionList.add(execution);
		if (execution.getStatus().equals(BatchStatus.FAILED)) {
			throwJobFailedException(Collections.singletonList(execution));
		}
	}

	private void monitorJobExecutions() {
		RepeatTemplate template = new RepeatTemplate();

		template.iterate(context -> {

			List<JobExecution> failedJobExecutions = new ArrayList<>();
			for (JobExecution jobExecution : this.jobExecutionList) {
				BatchStatus batchStatus = getCurrentBatchStatus(jobExecution);
				if (batchStatus.isRunning()) {
					Thread.sleep(this.taskBatchProperties.getFailOnJobFailurePollInterval());
					return RepeatStatus.CONTINUABLE;
				}
				if (batchStatus.equals(BatchStatus.FAILED)) {
					failedJobExecutions.add(jobExecution);
				}
			}

			if (failedJobExecutions.size() > 0) {
				throwJobFailedException(failedJobExecutions);
			}
			return RepeatStatus.FINISHED;
		});
	}

	private BatchStatus getCurrentBatchStatus(JobExecution jobExecution) {
		if (jobExecution.getStatus().isRunning()) {
			JobExecution jobExecutionWithStatus = this.taskJobRepository.getJobExecution(jobExecution.getId());
			Assert.state(jobExecutionWithStatus != null,
					"Job execution with id " + jobExecution.getId() + " not found");
			return jobExecutionWithStatus.getStatus();
		}
		return jobExecution.getStatus();
	}

	private void throwJobFailedException(List<JobExecution> failedJobExecutions) {
		StringBuilder message = new StringBuilder("The following Jobs have failed: \n");
		for (JobExecution failedJobExecution : failedJobExecutions) {
			message.append(String.format(
					"Job %s failed during " + "execution for job instance id %s with jobExecutionId of %s \n",
					failedJobExecution.getJobInstance().getJobName(), failedJobExecution.getId(),
					failedJobExecution.getId()));
		}

		logger.error(message);

		throw new TaskException(message.toString());

	}

	private JobParameters removeNonIdentifying(JobParameters parameters) {
		Set<JobParameter<?>> parameterMap = parameters.parameters();
		Set<JobParameter<?>> copy = new HashSet<>();

		for (JobParameter<?> parameter : parameterMap) {
			if (parameter.identifying()) {
				copy.add(parameter);
			}
		}

		return new JobParameters(copy);
	}

	private boolean isStoppedOrFailed(JobExecution execution) {
		BatchStatus status = execution.getStatus();
		return (status == BatchStatus.STOPPED || status == BatchStatus.FAILED);
	}

	private JobParameters merge(JobParameters parameters, JobParameters additionals) {
		Map<String, JobParameter<?>> merged = new HashMap<>();
		// Add base parameters
		for (JobParameter<?> param : parameters.parameters()) {
			merged.put(param.name(), param);
		}
		// Override with additionals
		for (JobParameter<?> param : additionals.parameters()) {
			merged.put(param.name(), param);
		}
		return new JobParameters(new HashSet<>(merged.values()));
	}

	/**
	 * Initializes the {@link JobParameters} based on the state of the {@link Job}. This
	 * should be called after all parameters have been entered into the builder. All
	 * parameters already set on this builder instance are appended to those retrieved
	 * from the job incrementer, overriding any with the same key (this is the same
	 * behavior as
	 * {@link org.springframework.batch.core.launch.support.CommandLineJobRunner} with the
	 * {@code -next} option and
	 * {@link org.springframework.batch.core.launch.JobOperator#startNextInstance(String)}).
	 * @param job The job for which the {@link JobParameters} are being constructed.
	 * @return a reference to this object.
	 *
	 * @since 4.0
	 */
	public JobParameters getNextJobParameters(Job job, Set<JobParameter<?>> parameterMap,
			JobRepository taskJobRepository) {
		Assert.notNull(job, "Job must not be null");
		Assert.notNull(job.getJobParametersIncrementer(),
				"No job parameters incrementer found for job=" + job.getName());

		String name = job.getName();
		JobParameters nextParameters;
		JobInstance lastInstance = taskJobRepository.getLastJobInstance(name);
		JobParametersIncrementer incrementer = job.getJobParametersIncrementer();
		if (lastInstance == null) {
			// Start from a completely clean sheet
			nextParameters = incrementer.getNext(new JobParameters());
		}
		else {
			JobExecution previousExecution = taskJobRepository.getLastJobExecution(lastInstance);
			if (previousExecution == null) {
				// Normally this will not happen - an instance exists with no executions
				nextParameters = incrementer.getNext(new JobParameters());
			}
			else {
				nextParameters = incrementer.getNext(previousExecution.getJobParameters());
			}
		}

		// start with parameters from the incrementer
		Set<JobParameter<?>> nextParametersMap = new HashSet<>(nextParameters.parameters());
		// append new parameters (overriding those with the same key)
		nextParametersMap.addAll(parameterMap);
		parameterMap = nextParametersMap;
		return new JobParameters(parameterMap);
	}

}
