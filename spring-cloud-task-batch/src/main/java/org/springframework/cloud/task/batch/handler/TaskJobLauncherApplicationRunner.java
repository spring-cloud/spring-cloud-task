/*
 * Copyright 2020-2020 the original author or authors.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.batch.JobExecutionEvent;
import org.springframework.boot.autoconfigure.batch.JobLauncherApplicationRunner;
import org.springframework.cloud.task.batch.configuration.TaskBatchProperties;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default and throws an exception upon the first job
 * that returns an {@link BatchStatus} of FAILED if a {@link TaskExecutor} in the
 * {@link JobLauncher} is not specified. If a {@link TaskExecutor} is specified in the
 * {@link JobLauncher} then all Jobs are launched and an exception is thrown if one or
 * more of the jobs has an {@link BatchStatus} of FAILED. TaskJobLauncherApplicationRunner
 * can also be used to launch a specific job by providing a jobName. The
 * TaskJobLauncherApplicationRunner takes the place of the
 * {@link JobLauncherApplicationRunner} when it is in use.
 *
 * @author Glenn Renfro
 * @since 2.3.0
 */
public class TaskJobLauncherApplicationRunner extends JobLauncherApplicationRunner
		implements ApplicationListener<JobExecutionEvent> {

	private static final Log logger = LogFactory.getLog(TaskJobLauncherApplicationRunner.class);

	private final JobExplorer taskJobExplorer;

	private final List<JobExecution> jobExecutionList = new ArrayList<>();

	private final TaskBatchProperties taskBatchProperties;

	/**
	 * Create a new {@link TaskJobLauncherApplicationRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 * @param taskBatchProperties the properties used to configure the
	 * taskBatchProperties.
	 */
	public TaskJobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
			JobRepository jobRepository, TaskBatchProperties taskBatchProperties) {
		super(jobLauncher, jobExplorer, jobRepository);
		this.taskJobExplorer = jobExplorer;
		this.taskBatchProperties = taskBatchProperties;
	}

	@Override
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
		monitorJobExecutions();
	}

	@Override
	protected void execute(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		// method is only kept to keep TaskJobLauncherApplicationRunnerCoreTests compiling
		super.execute(job, jobParameters);
	}

	@Override
	public void onApplicationEvent(JobExecutionEvent event) {
		JobExecution execution = event.getJobExecution();
		TaskJobLauncherApplicationRunner.this.jobExecutionList.add(execution);
		if (execution.getStatus().equals(BatchStatus.FAILED)) {
			throwJobFailedException(Collections.singletonList(execution));
		}
	}

	private void monitorJobExecutions() {
		RepeatTemplate template = new RepeatTemplate();

		template.iterate(context -> {

			List<JobExecution> failedJobExecutions = new ArrayList<>();
			RepeatStatus repeatStatus = RepeatStatus.FINISHED;
			for (JobExecution jobExecution : this.jobExecutionList) {
				JobExecution currentJobExecution = this.taskJobExplorer.getJobExecution(jobExecution.getId());
				BatchStatus batchStatus = currentJobExecution.getStatus();
				if (batchStatus.isRunning()) {
					repeatStatus = RepeatStatus.CONTINUABLE;
				}
				if (batchStatus.equals(BatchStatus.FAILED)) {
					failedJobExecutions.add(jobExecution);
				}
			}
			Thread.sleep(this.taskBatchProperties.getFailOnJobFailurePollInterval());

			if (repeatStatus.equals(RepeatStatus.FINISHED) && failedJobExecutions.size() > 0) {
				throwJobFailedException(failedJobExecutions);
			}
			return repeatStatus;
		});
	}

	private void throwJobFailedException(List<JobExecution> failedJobExecutions) {
		StringBuilder message = new StringBuilder("The following Jobs have failed: \n");
		for (JobExecution failedJobExecution : failedJobExecutions) {
			message.append(String.format(
					"Job %s failed during " + "execution for job instance id %s with jobExecutionId of %s \n",
					failedJobExecution.getJobInstance().getJobName(), failedJobExecution.getJobId(),
					failedJobExecution.getId()));
		}

		logger.error(message);

		throw new TaskException(message.toString());

	}

}
