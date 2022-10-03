/*
 * Copyright 2016-2019 the original author or authors.
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

import java.util.Date;
import java.util.Objects;

import org.springframework.batch.core.JobParameter;

/**
 * This is a JobParameter DTO created so that a
 * {@link org.springframework.batch.core.JobParameter} can be serialized into Json without
 * having to add mixins to an ObjectMapper.
 *
 * @author Glenn Renfro
 */
public class JobParameterEvent {

	private Object parameter;

	private boolean identifying;

	public JobParameterEvent() {
	}

	public JobParameterEvent(JobParameter jobParameter) {
		this.parameter = jobParameter.getValue();
		this.identifying = jobParameter.isIdentifying();
	}

	public boolean isIdentifying() {
		return this.identifying;
	}

	/**
	 * @return the value contained within this JobParameter.
	 */
	public Object getValue() {

		if (this.parameter != null && this.parameter.getClass().isInstance(Date.class)) {
			return new Date(((Date) this.parameter).getTime());
		}
		else {
			return this.parameter;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JobParameterEvent)) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		JobParameterEvent rhs = (JobParameterEvent) obj;
		return Objects.equals(this.parameter, rhs.parameter);
	}

	@Override
	public String toString() {
		return this.parameter == null ? null : this.parameter.toString();
	}

	@Override
	public int hashCode() {
		final int BASE_HASH = 7;
		final int MULTIPLIER_HASH = 21;
		return BASE_HASH + MULTIPLIER_HASH * this.parameter.hashCode();
	}

}
