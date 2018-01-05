/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener.support;

import org.springframework.batch.core.Entity;
import org.springframework.util.Assert;

/**
 * This is a JobInstance DTO created so that a {@link org.springframework.batch.core.JobInstance} can be serialized into
 * Json without having to add mixins to an ObjectMapper.
 *
 * @author Glenn Renfro
 */

public class JobInstanceEvent extends Entity {

	private String jobName;

	public JobInstanceEvent() {
		super(-1L);
	}

	public JobInstanceEvent(Long id, String jobName) {
		super(id);
		Assert.hasLength(jobName, "jobName must have length greater than zero.");
		this.jobName = jobName;
	}

	/**
	 * @return the job name. (Equivalent to getJob().getName())
	 */
	public String getJobName() {
		return jobName;
	}

	public String toString() {
		return super.toString() + ", Job=[" + jobName + "]";
	}

	public long getInstanceId() {
		return super.getId();
	}

}
