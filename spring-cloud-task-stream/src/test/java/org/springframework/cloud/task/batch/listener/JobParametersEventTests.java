/*
 *  Copyright 2016 the original author or authors.
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

package org.springframework.cloud.task.batch.listener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.cloud.task.batch.listener.support.JobParametersEvent;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

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
		assertEquals(0, jobParametersEvent.getParameters().size());
		assertTrue(jobParametersEvent.isEmpty());
	}

	@Test
	public void testConstructor() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertEquals(STRING_PARAM.getValue(), jobParametersEvent.getString(STRING_KEY));
		assertEquals(LONG_PARAM.getValue(), jobParametersEvent.getLong(LONG_KEY));
		assertEquals(DATE_PARAM.getValue(), jobParametersEvent.getDate(DATE_KEY));
		assertEquals(DOUBLE_PARAM.getValue(), jobParametersEvent.getDouble(DOUBLE_KEY));

		JobParametersEvent jobParametersEventNew = getPopulatedParametersEvent();
		assertEquals(jobParametersEventNew, jobParametersEvent);
	}

	@Test
	public void testEquals() {
		assertTrue(getPopulatedParametersEvent().equals(getPopulatedParametersEvent()));
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertFalse(jobParametersEvent.equals("FOO"));
		assertTrue(jobParametersEvent.equals(jobParametersEvent));
	}

	@Test
	public void testHashCode() {
		JobParametersEvent jobParametersEvent = new JobParametersEvent();
		assertNotNull(jobParametersEvent.hashCode());
		JobParametersEvent jobParametersEventPopulated = getPopulatedParametersEvent();
		assertNotNull(jobParametersEvent);
		assertNotEquals(jobParametersEvent.hashCode(), jobParametersEventPopulated.hashCode());
	}

	@Test
	public void testToProperties() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		Properties properties = jobParametersEvent.toProperties();
		assertEquals(properties.getProperty(DATE_KEY), jobParametersEvent.getString(DATE_KEY));
		assertEquals(properties.getProperty(STRING_KEY), jobParametersEvent.getString(STRING_KEY));
		assertEquals(properties.getProperty(LONG_KEY), jobParametersEvent.getString(LONG_KEY));
		assertEquals(properties.getProperty(DOUBLE_KEY), jobParametersEvent.getString(DOUBLE_KEY));
	}

	@Test
	public void testToString() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertNotNull(toString());
	}

	@Test
	public void testGetterSetterDefaults() {
		JobParametersEvent jobParametersEvent = getPopulatedParametersEvent();
		assertEquals(new Double(0), jobParametersEvent.getDouble("FOOBAR"));
		assertEquals(new Long(0), jobParametersEvent.getLong("FOOBAR"));
		assertEquals(new Double(5), jobParametersEvent.getDouble("FOOBAR", 5));
		assertEquals(DOUBLE_PARAM.getValue(), jobParametersEvent.getDouble(DOUBLE_KEY, 0));
		assertEquals(new Long(5), jobParametersEvent.getLong("FOOBAR", 5));
		assertEquals(LONG_PARAM.getValue(), jobParametersEvent.getLong(LONG_KEY, 5));
		assertEquals("TESTVAL", jobParametersEvent.getString("FOOBAR","TESTVAL"));
		assertEquals(STRING_PARAM.getValue(),
				jobParametersEvent.getString(STRING_KEY,"TESTVAL"));

		Date date = new Date();
		assertEquals(date, jobParametersEvent.getDate("FOOBAR", date));
		assertEquals(DATE_PARAM.getValue(),
				jobParametersEvent.getDate(DATE_KEY, date));

	}

	public JobParametersEvent getPopulatedParametersEvent() {
		Map<String, JobParameter> jobParameters = new HashMap<>();
		jobParameters.put(DATE_KEY, DATE_PARAM);
		jobParameters.put(STRING_KEY,STRING_PARAM);
		jobParameters.put(LONG_KEY,LONG_PARAM);
		jobParameters.put(DOUBLE_KEY,DOUBLE_PARAM);
		return new JobParametersEvent(jobParameters);
	}

}
