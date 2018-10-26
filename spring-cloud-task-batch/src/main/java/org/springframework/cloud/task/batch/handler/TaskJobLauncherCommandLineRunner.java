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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
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

	private static final Log logger = LogFactory
			.getLog(TaskJobLauncherCommandLineRunner.class);

	private List<JobExecution> jobExecutionList = new ArrayList<>();

	private ApplicationEventPublisher taskApplicationEventPublisher;

	private TaskBatchProperties taskBatchProperties;

	public TaskJobLauncherCommandLineRunner(JobLauncher jobLauncher,
			JobExplorer jobExplorer, TaskBatchProperties taskBatchProperties) {
		super(jobLauncher, jobExplorer);
		this.taskJobLauncher = jobLauncher;
		this.taskJobExplorer = jobExplorer;
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
		validateJobExecutions();
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		JobParameters nextParameters = new JobParametersBuilder(jobParameters,
				this.taskJobExplorer).getNextJobParameters(job).toJobParameters();
		JobExecution execution = this.taskJobLauncher.run(job, nextParameters);
		if (this.taskApplicationEventPublisher != null) {
			this.taskApplicationEventPublisher.publishEvent(new JobExecutionEvent(execution));
		}
		this.jobExecutionList.add(execution);
		if (execution.getStatus().equals(BatchStatus.FAILED)) {
			throwJobFailedException(Collections.singletonList(execution));
		}
	}

	private void validateJobExecutions() {
		RepeatTemplate template = new RepeatTemplate();

		Date startDate = new Date();

		template.iterate(new RepeatCallback() {

			public RepeatStatus doInIteration(RepeatContext context) throws InterruptedException {
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
				if (repeatStatus.isContinuable() && taskBatchProperties.getFailOnJobFailurewaitTime() != 0
						&& (new Date()).getTime() - startDate.getTime() > taskBatchProperties.getFailOnJobFailurewaitTime()) {
					throw new IllegalStateException("Not all jobs were completed " +
							"within the time specified by spring.cloud.task.batch." +
							"failOnJobFailurewaitTime.");
				}
				return repeatStatus;
			}

		});
	}

	public void throwJobFailedException(List<JobExecution> failedJobExecutions) {
		String message = "The following Jobs have failed: \n";
		for (JobExecution failedJobExecution : failedJobExecutions) {
			message += String.format("Job %s failed during " +
					"execution for jobId %s with jobExecutionId of %s \n",
					failedJobExecution.getJobInstance().getJobName(),
					failedJobExecution.getJobId(), failedJobExecution.getId());
		}
		logger.error(message);
		throw new TaskException(message);

	}

}
