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

package org.springframework.cloud.task.batch.listener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.cloud.task.batch.listener.support.JobParametersEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class JobParametersEventTests {

	private final static JobParameter STRING_PARAM = new JobParameter("FOO", true);

	private final static JobParameter DATE_PARAM = new JobParameter(new Date(), true);

	private final static JobParameter LONG_PARAM = new JobParameter(1L, true);

	private final static JobParameter DOUBLE_PARAM = new JobParameter(2D, true);

	private final static String DATE_KEY = "DATE_KEY";

	private final static String STRING_KEY = "STRING_KEY";

	private final static String LONG_KEY = "LONG_KEY";

	private final static String DOUBLE_KEY = "DOUBLE_KEY";

	@Test
	public void testDefaultConstructor() {
		JobParametersEvent jobParametersEvent = new JobParametersEvent();
		assertThat(jobParametersEvent.getParameters().size()).isEqualTo(0);
		assertThat(jobParametersEvent.isEmpty()).isTrue();
	}

	@Test
	public void testConstructor() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertThat(jobParametersEvent.getString(STRING_KEY))
				.isEqualTo(STRING_PARAM.getValue());
		assertThat(jobParametersEvent.getLong(LONG_KEY)).isEqualTo(LONG_PARAM.getValue());
		assertThat(jobParametersEvent.getDate(DATE_KEY)).isEqualTo(DATE_PARAM.getValue());
		assertThat(jobParametersEvent.getDouble(DOUBLE_KEY))
				.isEqualTo(DOUBLE_PARAM.getValue());

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
		assertThat(jobParametersEventPopulated.hashCode())
				.isNotEqualTo(jobParametersEvent.hashCode());
	}

	@Test
	public void testToProperties() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		Properties properties = jobParametersEvent.toProperties();
		assertThat(jobParametersEvent.getString(DATE_KEY))
				.isEqualTo(properties.getProperty(DATE_KEY));
		assertThat(jobParametersEvent.getString(STRING_KEY))
				.isEqualTo(properties.getProperty(STRING_KEY));
		assertThat(jobParametersEvent.getString(LONG_KEY))
				.isEqualTo(properties.getProperty(LONG_KEY));
		assertThat(jobParametersEvent.getString(DOUBLE_KEY))
				.isEqualTo(properties.getProperty(DOUBLE_KEY));
	}

	@Test
	public void testToString() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertThat(toString()).isNotNull();
	}

	@Test
	public void testGetterSetterDefaults() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertThat(jobParametersEvent.getDouble("FOOBAR")).isEqualTo(new Double(0));
		assertThat(jobParametersEvent.getLong("FOOBAR")).isEqualTo(new Long(0));
		assertThat(jobParametersEvent.getDouble("FOOBAR", 5)).isEqualTo(new Double(5));
		assertThat(jobParametersEvent.getDouble(DOUBLE_KEY, 0))
				.isEqualTo(DOUBLE_PARAM.getValue());
		assertThat(jobParametersEvent.getLong("FOOBAR", 5)).isEqualTo(new Long(5));
		assertThat(jobParametersEvent.getLong(LONG_KEY, 5))
				.isEqualTo(LONG_PARAM.getValue());
		assertThat(jobParametersEvent.getString("FOOBAR", "TESTVAL"))
				.isEqualTo("TESTVAL");
		assertThat(jobParametersEvent.getString(STRING_KEY, "TESTVAL"))
				.isEqualTo(STRING_PARAM.getValue());

		Date date = new Date();
		assertThat(jobParametersEvent.getDate("FOOBAR", date)).isEqualTo(date);
		assertThat(jobParametersEvent.getDate(DATE_KEY, date))
				.isEqualTo(DATE_PARAM.getValue());

	}

	public JobParametersEvent getPopulatedParametersEvent() {
		Map<String, JobParameter> jobParameters = new HashMap<>();
		jobParameters.put(DATE_KEY, DATE_PARAM);
		jobParameters.put(STRING_KEY, STRING_PARAM);
		jobParameters.put(LONG_KEY, LONG_PARAM);
		jobParameters.put(DOUBLE_KEY, DOUBLE_PARAM);
		return new JobParametersEvent(jobParameters);
	}

}
