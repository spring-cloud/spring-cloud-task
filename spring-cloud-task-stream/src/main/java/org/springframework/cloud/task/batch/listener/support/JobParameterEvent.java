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

import java.util.*;


/**
 * This is a JobParameter DTO created so that a {@link org.springframework.batch.core.JobParameter} can be serialized
 * into Json without having to add mixins to an ObjectMapper.
 * @author Glenn Renfro
 */
public class JobParameterEvent {
	private Object parameter;

	private JobParameterEvent.ParameterType parameterType;

	private boolean identifying;

	public JobParameterEvent() {

	}

	/**
	 * Construct a new JobParameter as a String.
	 */
	public JobParameterEvent(String parameter, boolean identifying) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.STRING;
		this.identifying = identifying;
	}

	/**
	 * Construct a new JobParameter as a Long.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Long parameter, boolean identifying) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.LONG;
		this.identifying = identifying;
	}

	/**
	 * Construct a new JobParameter as a Date.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Date parameter, boolean identifying) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.DATE;
		this.identifying = identifying;
	}

	/**
	 * Construct a new JobParameter as a Double.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Double parameter, boolean identifying) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.DOUBLE;
		this.identifying = identifying;
	}


	/**
	 * Construct a new JobParameter as a String.
	 */
	public JobParameterEvent(String parameter) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.STRING;
		this.identifying = true;
	}

	/**
	 * Construct a new JobParameter as a Long.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Long parameter) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.LONG;
		this.identifying = true;
	}

	/**
	 * Construct a new JobParameter as a Date.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Date parameter) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.DATE;
		this.identifying = true;
	}

	/**
	 * Construct a new JobParameter as a Double.
	 *
	 * @param parameter
	 */
	public JobParameterEvent(Double parameter) {
		this.parameter = parameter;
		parameterType = JobParameterEvent.ParameterType.DOUBLE;
		this.identifying = true;
	}

	public boolean isIdentifying() {
		return identifying;
	}

	/**
	 * @return the value contained within this JobParameter.
	 */
	public Object getValue() {

		if (parameter != null && parameter.getClass().isInstance(Date.class)) {
			return new Date(((Date) parameter).getTime());
		}
		else {
			return parameter;
		}
	}

	/**
	 * @return a ParameterType representing the type of this parameter.
	 */
	public JobParameterEvent.ParameterType getType() {
		return parameterType;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JobParameterEvent == false) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		JobParameterEvent rhs = (JobParameterEvent) obj;
		return parameter==null ? rhs.parameter==null && parameterType==rhs.parameterType: parameter.equals(rhs.parameter);
	}

	@Override
	public String toString() {
		return parameter == null ? null : (parameterType == JobParameterEvent.ParameterType.DATE ? "" + ((Date) parameter).getTime()
				: parameter.toString());
	}

	@Override
	public int hashCode() {
		return 7 + 21 * (parameter == null ? parameterType.hashCode() : parameter.hashCode());
	}

	/**
	 * Enumeration representing the type of a JobParameter.
	 */
	public enum ParameterType {

		STRING, DATE, LONG, DOUBLE;
	}
}
