/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.cloud.task.batch.configuration.TaskBatchProperties;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;

/**
 * {@link CommandLineRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default and throws an exception upon the first job
 * that returns an {@link BatchStatus} of FAILED if a {@link TaskExecutor} in the
 * {@link JobLauncher} is not specified. If a {@link TaskExecutor} is specified
 * in the {@link JobLauncher} then all Jobs are launched and an
 * exception is thrown if one or more of the jobs has an {@link BatchStatus} of FAILED.
 * TaskJobLauncherCommandLineRunner can also be used to launch a specific job by
 * providing a jobName. The TaskJobLaunchercommandLineRunner takes the place of the
 * {@link org.springframework.boot.autoconfigure.batch.JobLauncherCommandLineRunner} when
 * it is in use.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
public class TaskJobLauncherCommandLineRunner extends JobLauncherCommandLineRunner {

	private JobLauncher taskJobLauncher;

	private JobExplorer taskJobExplorer;

	private JobRepository taskJobRepository;

	private static final Log logger = LogFactory
			.getLog(TaskJobLauncherCommandLineRunner.class);

	private List<JobExecution> jobExecutionList = new ArrayList<>();

	private ApplicationEventPublisher taskApplicationEventPublisher;

	private TaskBatchProperties taskBatchProperties;

	/**
	 * Create a new {@link TaskJobLauncherCommandLineRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 * @param taskBatchProperties the properties used to configure the taskBatchProperties.
	 */
	public TaskJobLauncherCommandLineRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
			JobRepository jobRepository, TaskBatchProperties taskBatchProperties) {
		super(jobLauncher, jobExplorer, jobRepository);
		this.taskJobLauncher = jobLauncher;
		this.taskJobExplorer = jobExplorer;
		this.taskJobRepository = jobRepository;
		this.taskBatchProperties = taskBatchProperties;
	}

	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		super.setApplicationEventPublisher(publisher);
		this.taskApplicationEventPublisher = publisher;
	}

	@Override
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
		monitorJobExecutions();
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		String jobName = job.getName();
		JobParameters parameters = jobParameters;
		boolean jobInstanceExists = this.taskJobRepository.isJobInstanceExists(jobName,
				parameters);
		if (jobInstanceExists) {
			JobExecution lastJobExecution = this.taskJobRepository
					.getLastJobExecution(jobName, jobParameters);
			if (lastJobExecution != null && isStoppedOrFailed(lastJobExecution)
					&& job.isRestartable()) {
				// Retry a failed or stopped execution with previous parameters
				JobParameters previousParameters = lastJobExecution.getJobParameters();
				/*
				 * remove Non-identifying parameters from the previous execution's
				 * parameters since there is no way to remove them programmatically. If
				 * they are required (or need to be modified) on a restart, they need to
				 * be (re)specified.
				 */
				JobParameters previousIdentifyingParameters = removeNonIdentifying(
						previousParameters);
				// merge additional parameters with previous ones (overriding those with
				// the same key)
				parameters = merge(previousIdentifyingParameters, jobParameters);
			}
		}
		else {
			JobParametersIncrementer incrementer = job.getJobParametersIncrementer();
			if (incrementer != null) {
				JobParameters nextParameters = new JobParametersBuilder(jobParameters,
						this.taskJobExplorer).getNextJobParameters(job).toJobParameters();
				parameters = merge(nextParameters, jobParameters);
			}
		}
		JobExecution execution = this.taskJobLauncher.run(job, parameters);
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

		Date startDate = new Date();

		template.iterate(context -> {

			List<JobExecution> failedJobExecutions = new ArrayList<>();
			RepeatStatus repeatStatus = RepeatStatus.FINISHED;
			for (JobExecution jobExecution : jobExecutionList) {
				JobExecution currentJobExecution = taskJobExplorer.getJobExecution(jobExecution.getId());
				BatchStatus batchStatus = currentJobExecution.getStatus();
				if (batchStatus.isRunning()) {
					repeatStatus = RepeatStatus.CONTINUABLE;
				}
				if (batchStatus.equals(BatchStatus.FAILED)) {
					failedJobExecutions.add(jobExecution);
				}
			}
			Thread.sleep(taskBatchProperties.getFailOnJobFailurePollInterval());

			if (repeatStatus.equals(RepeatStatus.FINISHED) && failedJobExecutions.size() > 0) {
				throwJobFailedException(failedJobExecutions);
			}
			return repeatStatus;
		});
	}

	private void throwJobFailedException(List<JobExecution> failedJobExecutions) {
		StringBuilder message = new StringBuilder("The following Jobs have failed: \n");
		for (JobExecution failedJobExecution : failedJobExecutions) {
			message.append(String.format("Job %s failed during " +
					"execution for job instance id %s with jobExecutionId of %s \n",
					failedJobExecution.getJobInstance().getJobName(),
					failedJobExecution.getJobId(), failedJobExecution.getId()));
		}

		logger.error(message);

		throw new TaskException(message.toString());

	}
	private JobParameters removeNonIdentifying(JobParameters parameters) {
		Map<String, JobParameter> parameterMap = parameters.getParameters();
		HashMap<String, JobParameter> copy = new HashMap<>(parameterMap);

		for (Map.Entry<String, JobParameter> parameter : copy.entrySet()) {
			if (!parameter.getValue().isIdentifying()) {
				parameterMap.remove(parameter.getKey());
			}
		}

		return new JobParameters(parameterMap);
	}

	private boolean isStoppedOrFailed(JobExecution execution) {
		BatchStatus status = execution.getStatus();
		return (status == BatchStatus.STOPPED || status == BatchStatus.FAILED);
	}

	private JobParameters merge(JobParameters parameters, JobParameters additionals) {
		Map<String, JobParameter> merged = new HashMap<>();
		merged.putAll(parameters.getParameters());
		merged.putAll(additionals.getParameters());
		return new JobParameters(merged);
	}
}
