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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Establish properties to be used for how Tasks work with
 * Spring Batch.
 *
 * @author Glenn Renfro
 */
@ConfigurationProperties(prefix = "spring.cloud.task.batch")
public class TaskBatchProperties {

	/**
	 * Comma-separated list of job names to execute on startup (for instance,
	 * `job1,job2`). By default, all Jobs found in the context are executed.
	 */
	private String jobNames = "";

	public String getJobNames() {
		return this.jobNames;
	}

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
	}
}
