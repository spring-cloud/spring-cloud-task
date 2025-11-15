/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.cloud.task.batch.listener.support;

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.job.parameters.JobParameter;

/**
 * This is a JobParametersEvent DTO created so that a
 * {@link org.springframework.batch.core.job.parameters.JobParameters} can be serialized
 * into Json without having to add mixins to an ObjectMapper.
 *
 * @author Glenn Renfro
 */
public class JobParametersEvent {

	private final Set<JobParameterEvent> parameters;

	public JobParametersEvent() {
		this.parameters = new HashSet<>();
	}

	public JobParametersEvent(Set<JobParameter<?>> jobParameters) {
		this.parameters = new HashSet<>();
		for (JobParameter<?> entry : jobParameters) {
			this.parameters.add(new JobParameterEvent(entry));
		}
	}

	/**
	 * Get a map of all parameters, including string, long, and date.
	 * @return an unmodifiable map containing all parameters.
	 */
	public Set<JobParameterEvent> getParameters() {
		return new HashSet<>(this.parameters);
	}

	/**
	 * @return true if the parameters is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return this.parameters.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JobParametersEvent)) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		JobParametersEvent rhs = (JobParametersEvent) obj;
		return this.parameters.equals(rhs.parameters);
	}

	@Override
	public int hashCode() {
		final int BASE_HASH = 17;
		final int MULTIPLIER_HASH = 23;
		return BASE_HASH + MULTIPLIER_HASH * this.parameters.hashCode();
	}

	@Override
	public String toString() {
		return this.parameters.toString();
	}

}
