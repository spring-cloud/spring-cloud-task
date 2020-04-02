/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure the step and job level properties for a single step job.
 *
 * @author Michael Minella
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job")
public class SingleStepJobProperties {

	private String stepName;

	private Integer chunkSize;

	private String jobName;

	/**
	 * Name of the step in the single step job.
	 * @return name
	 */
	public String getStepName() {
		return stepName;
	}

	/**
	 * Set the name of the step.
	 * @param stepName name
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * The number of items to process per transaction/chunk.
	 * @return number of items
	 */
	public Integer getChunkSize() {
		return chunkSize;
	}

	/**
	 * Set the number of items within a transaction/chunk.
	 * @param chunkSize number of items
	 */
	public void setChunkSize(Integer chunkSize) {
		this.chunkSize = chunkSize;
	}

	/**
	 * The name of the job.
	 * @return name
	 */
	public String getJobName() {
		return jobName;
	}

	/**
	 * Set the name of the job.
	 * @param jobName name
	 */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

}
