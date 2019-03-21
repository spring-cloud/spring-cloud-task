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

import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.cloud.task.batch.listener.support.JobParameterEvent;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class JobParameterEventTests {

	@Test
	public void testDefaultConstructor() {
		JobParameterEvent jobParameterEvent = new JobParameterEvent();
		assertThat(jobParameterEvent.getValue()).isNull();
		assertThat(jobParameterEvent.getType()).isNull();
		assertFalse(jobParameterEvent.isIdentifying());
		assertThat(jobParameterEvent).isEqualTo(new JobParameterEvent());
	}

	@Test
	public void testConstructor() {
		final String EXPECTED_VALUE = "FOO";
		final Date EXPECTED_DATE_VALUE = new Date();
		JobParameter jobParameter = new JobParameter(EXPECTED_VALUE, true);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.getValue()).isEqualTo(EXPECTED_VALUE);
		assertThat(jobParameterEvent.getType())
				.isEqualTo(JobParameterEvent.ParameterType.STRING);
		assertTrue(jobParameterEvent.isIdentifying());

		jobParameter = new JobParameter(EXPECTED_DATE_VALUE, true);
		jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.getValue()).isEqualTo(EXPECTED_DATE_VALUE);
		assertThat(jobParameterEvent.getType())
				.isEqualTo(JobParameterEvent.ParameterType.DATE);
		assertTrue(jobParameterEvent.isIdentifying());
		assertTrue(new JobParameterEvent(jobParameter).equals(jobParameterEvent));
	}

	@Test
	public void testEquals() {
		final String EXPECTED_VALUE = "FOO";
		JobParameter jobParameter = new JobParameter(EXPECTED_VALUE, true);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		JobParameterEvent anotherJobParameterEvent = new JobParameterEvent(jobParameter);

		assertTrue(jobParameterEvent.equals(jobParameterEvent));
		assertFalse(jobParameterEvent.equals("nope"));
		assertTrue(jobParameterEvent.equals(anotherJobParameterEvent));
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidHashCode() {
		JobParameterEvent jobParameterEvent = new JobParameterEvent();
		assertThat(jobParameterEvent.hashCode()).isNull();
		final String EXPECTED_VALUE = "FOO";
		JobParameter jobParameter = new JobParameter(EXPECTED_VALUE, true);
		jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.hashCode()).isNotNull();
	}

	@Test
	public void testValidHashCode() {
		final String EXPECTED_VALUE = "FOO";
		JobParameter jobParameter = new JobParameter(EXPECTED_VALUE, true);
		JobParameterEvent jobParameterEvent = new JobParameterEvent(jobParameter);
		assertThat(jobParameterEvent.hashCode()).isNotNull();
	}

}
