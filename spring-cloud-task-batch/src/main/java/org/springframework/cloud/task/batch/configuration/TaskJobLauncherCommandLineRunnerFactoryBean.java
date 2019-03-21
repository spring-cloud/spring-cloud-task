/*
 * Copyright 2018-2019 the original author or authors.
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
import org.springframework.cloud.task.batch.handler.TaskJobLauncherCommandLineRunner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating an instance of {@link TaskJobLauncherCommandLineRunner}.
 *
 * @author Glenn Renfro
 */
public class TaskJobLauncherCommandLineRunnerFactoryBean
		implements FactoryBean<TaskJobLauncherCommandLineRunner> {

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	private List<Job> jobs;

	private String jobNames;

	private JobRegistry jobRegistry;

	private Integer order = 0;

	private TaskBatchProperties taskBatchProperties;

	private JobRepository jobRepository;

	public TaskJobLauncherCommandLineRunnerFactoryBean(JobLauncher jobLauncher,
			JobExplorer jobExplorer, List<Job> jobs,
			TaskBatchProperties taskBatchProperties, JobRegistry jobRegistry,
			JobRepository jobRepository) {
		Assert.notNull(taskBatchProperties, "properties must not be null");
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		Assert.notEmpty(jobs, "jobs must not be null nor empty");
		this.jobs = jobs;
		this.jobNames = taskBatchProperties.getJobNames();
		this.jobRegistry = jobRegistry;
		this.taskBatchProperties = taskBatchProperties;
		this.order = taskBatchProperties.getCommandLineRunnerOrder();
		this.jobRepository = jobRepository;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public TaskJobLauncherCommandLineRunner getObject() {
		TaskJobLauncherCommandLineRunner taskJobLauncherCommandLineRunner = new TaskJobLauncherCommandLineRunner(
				this.jobLauncher, this.jobExplorer, this.jobRepository,
				this.taskBatchProperties);
		taskJobLauncherCommandLineRunner.setJobs(this.jobs);
		if (StringUtils.hasText(this.jobNames)) {
			taskJobLauncherCommandLineRunner.setJobNames(this.jobNames);
		}
		taskJobLauncherCommandLineRunner.setJobRegistry(this.jobRegistry);

		if (this.order != null) {
			taskJobLauncherCommandLineRunner.setOrder(this.order);
		}
		return taskJobLauncherCommandLineRunner;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskJobLauncherCommandLineRunner.class;
	}

}
