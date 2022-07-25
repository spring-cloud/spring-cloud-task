/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.task.batch.configuration;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherApplicationRunner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating an instance of {@link TaskJobLauncherApplicationRunner}.
 *
 * @author Glenn Renfro
 * @since 2.3.0
 */
public class TaskJobLauncherApplicationRunnerFactoryBean implements FactoryBean<TaskJobLauncherApplicationRunner> {

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	private List<Job> jobs;

	private String jobName;

	private JobRegistry jobRegistry;

	private Integer order = 0;

	private TaskBatchProperties taskBatchProperties;

	private JobRepository jobRepository;

	public TaskJobLauncherApplicationRunnerFactoryBean(JobLauncher jobLauncher, JobExplorer jobExplorer, List<Job> jobs,
			TaskBatchProperties taskBatchProperties, JobRegistry jobRegistry, JobRepository jobRepository,
			BatchProperties batchProperties) {
		Assert.notNull(taskBatchProperties, "taskBatchProperties must not be null");
		Assert.notNull(batchProperties, "batchProperties must not be null");
		Assert.notEmpty(jobs, "jobs must not be null nor empty");

		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		this.jobs = jobs;
		this.jobName = taskBatchProperties.getJobNames();
		this.jobRegistry = jobRegistry;
		this.taskBatchProperties = taskBatchProperties;
		if (StringUtils.hasText(batchProperties.getJob().getName())) {
			this.jobName = batchProperties.getJob().getName();
		}
		else {
			this.jobName = taskBatchProperties.getJobNames();
		}
		this.order = taskBatchProperties.getCommandLineRunnerOrder();
		this.jobRepository = jobRepository;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public TaskJobLauncherApplicationRunner getObject() {
		TaskJobLauncherApplicationRunner taskJobLauncherApplicationRunner = new TaskJobLauncherApplicationRunner(
				this.jobLauncher, this.jobExplorer, this.jobRepository, this.taskBatchProperties);
		taskJobLauncherApplicationRunner.setJobs(this.jobs);
		if (StringUtils.hasText(this.jobName)) {
			taskJobLauncherApplicationRunner.setJobName(this.jobName);
		}
		taskJobLauncherApplicationRunner.setJobRegistry(this.jobRegistry);

		if (this.order != null) {
			taskJobLauncherApplicationRunner.setOrder(this.order);
		}
		return taskJobLauncherApplicationRunner;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskJobLauncherApplicationRunner.class;
	}

}
