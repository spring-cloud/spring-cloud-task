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

package org.springframework.cloud.task.batch.configuration;

import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.batch.handler.TaskJobLauncherCommandLineRunner;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating an instance of {@link TaskJobLauncherCommandLineRunner}.
 *
 * @author Glenn Renfro
 */
public class TaskJobLauncherCommandLineRunnerFactoryBean implements FactoryBean<TaskJobLauncherCommandLineRunner> {

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	private List<Job> jobs;

	private String jobNames;

	private JobRegistry jobRegistry;

	private Integer order = 0;

	/**
	 * Maximum wait time that Spring Cloud task will wait for tasks to complete
	 * when spring.cloud.task.batch.failOnJobFailure is set to true.  Defaults
	 * to 0.  0 indicates no wait time is enforced.
	 */
	private long failOnJobFailurewaitTimeInMillis = 0;

	/**
	 * Fixed delay that Spring Cloud Task will wait when checking if
	 * {@link org.springframework.batch.core.JobExecution}s have completed,
	 * when spring.cloud.task.batch.failOnJobFailure is set to true.  Defaults
	 * to 5000.
	 */
	private long failOnJobFailurePollIntervalInMillis = 5000l;

	public TaskJobLauncherCommandLineRunnerFactoryBean(JobLauncher jobLauncher,
			JobExplorer jobExplorer, List<Job> jobs, String jobNames,
			JobRegistry jobRegistry) {
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		Assert.notEmpty(jobs, "jobs must not be null nor empty");
		this.jobs = jobs;
		this.jobNames = jobNames;
		this.jobRegistry = jobRegistry;
	}

	public TaskJobLauncherCommandLineRunnerFactoryBean(JobLauncher jobLauncher,
			JobExplorer jobExplorer, List<Job> jobs, TaskBatchProperties properties,
			JobRegistry jobRegistry) {
		Assert.notNull(properties, "properties must not be null");
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		Assert.notEmpty(jobs, "jobs must not be null nor empty");
		this.jobs = jobs;
		this.jobNames = properties.getJobNames();
		this.jobRegistry = jobRegistry;
		this.failOnJobFailurePollIntervalInMillis = properties.getFailOnJobFailurePollIntervalInMillis();
		this.failOnJobFailurewaitTimeInMillis = properties.getFailOnJobFailurewaitTimeInMillis();
		this.order = properties.getCommandLineRunnerOrder();
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public TaskJobLauncherCommandLineRunner getObject() throws Exception {
		TaskJobLauncherCommandLineRunner  taskJobLauncherCommandLineRunner =
				new TaskJobLauncherCommandLineRunner(this.jobLauncher, this.jobExplorer);
		taskJobLauncherCommandLineRunner.setJobs(this.jobs);
		if(StringUtils.hasText(this.jobNames)) {
			taskJobLauncherCommandLineRunner.setJobNames(this.jobNames);
		}
		taskJobLauncherCommandLineRunner.setJobRegistry(this.jobRegistry);

		if(this.order != null) {
			taskJobLauncherCommandLineRunner.setOrder(this.order);
		}
		taskJobLauncherCommandLineRunner.setFailOnJobFailurePollIntervalInMillis(this.failOnJobFailurePollIntervalInMillis);
		taskJobLauncherCommandLineRunner.setFailOnJobFailurewaitTimeInMillis(this.failOnJobFailurewaitTimeInMillis);
		return taskJobLauncherCommandLineRunner;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskJobLauncherCommandLineRunner.class;
	}

	public long getFailOnJobFailurewaitTimeInMillis() {
		return failOnJobFailurewaitTimeInMillis;
	}

	public void setFailOnJobFailurewaitTimeInMillis(long failOnJobFailurewaitTimeInMillis) {
		this.failOnJobFailurewaitTimeInMillis = failOnJobFailurewaitTimeInMillis;
	}

	public long getFailOnJobFailurePollIntervalInMillis() {
		return failOnJobFailurePollIntervalInMillis;
	}

	public void setFailOnJobFailurePollIntervalInMillis(long failOnJobFailurePollIntervalInMillis) {
		this.failOnJobFailurePollIntervalInMillis = failOnJobFailurePollIntervalInMillis;
	}
}
