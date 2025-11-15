/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.cloud.task.batch.listener.support.JobParameterEvent;
import org.springframework.cloud.task.batch.listener.support.JobParametersEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class JobParametersEventTests {

	private final static String DATE_KEY = "DATE_KEY";

	private final static String STRING_KEY = "STRING_KEY";

	private final static String LONG_KEY = "LONG_KEY";

	private final static String DOUBLE_KEY = "DOUBLE_KEY";

	private final static JobParameter<String> STRING_PARAM = new JobParameter<>(STRING_KEY, "FOO", String.class);

	private final static JobParameter<Date> DATE_PARAM = new JobParameter<>(DATE_KEY, new Date(), Date.class);

	private final static JobParameter<Long> LONG_PARAM = new JobParameter<>(LONG_KEY, 1L, Long.class);

	private final static JobParameter<Double> DOUBLE_PARAM = new JobParameter<>(DOUBLE_KEY, 2D, Double.class);

	@Test
	public void testDefaultConstructor() {
		JobParametersEvent jobParametersEvent = new JobParametersEvent();
		assertThat(jobParametersEvent.getParameters().size()).isEqualTo(0);
		assertThat(jobParametersEvent.isEmpty()).isTrue();
	}

	@Test
	public void testConstructor() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		Set<JobParameterEvent> jobParameters = jobParametersEvent.getParameters();
		assertThat(jobParametersEvent.getParameters()).contains(new JobParameterEvent(STRING_PARAM),
				new JobParameterEvent(DATE_PARAM), new JobParameterEvent(LONG_PARAM),
				new JobParameterEvent(DOUBLE_PARAM));
		JobParametersEvent jobParametersEventNew = getPopulatedParametersEvent();
		assertThat(jobParametersEvent).isEqualTo(jobParametersEventNew);
	}

	@Test
	public void testEquals() {
		assertThat(getPopulatedParametersEvent().equals(getPopulatedParametersEvent())).isTrue();
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertThat(jobParametersEvent.equals("FOO")).isFalse();
		assertThat(jobParametersEvent.equals(jobParametersEvent)).isTrue();
	}

	@Test
	public void testHashCode() {
		JobParametersEvent jobParametersEvent = new JobParametersEvent();
		assertThat(jobParametersEvent.hashCode()).isNotNull();
		JobParametersEvent jobParametersEventPopulated = getPopulatedParametersEvent();
		assertThat(jobParametersEvent).isNotNull();
		assertThat(jobParametersEventPopulated.hashCode()).isNotEqualTo(jobParametersEvent.hashCode());
	}

	@Test
	public void testToString() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertThat(toString()).isNotNull();
	}

	public JobParametersEvent getPopulatedParametersEvent() {
		Set<JobParameter<?>> jobParameters = new HashSet<>();
		jobParameters.add(DATE_PARAM);
		jobParameters.add(STRING_PARAM);
		jobParameters.add(LONG_PARAM);
		jobParameters.add(DOUBLE_PARAM);
		return new JobParametersEvent(jobParameters);
	}

}
