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

import org.junit.Test;

import org.springframework.cloud.task.batch.listener.support.JobInstanceEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Glenn Renfro
 */
public class JobInstanceEventTests {

	public static final long INSTANCE_ID = 1;
	public static final String JOB_NAME = "FOOBAR";

	@Test
	public void testConstructor() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent(INSTANCE_ID, JOB_NAME);
		assertEquals(INSTANCE_ID, jobInstanceEvent.getInstanceId());
		assertEquals(JOB_NAME, jobInstanceEvent.getJobName());
	}

	@Test
	public void testEmptyConstructor() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent();
		assertNull(jobInstanceEvent.getJobName());
	}

	@Test
	public void testEmptyConstructorEmptyId() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent();
		jobInstanceEvent.getInstanceId();
	}

	@Test
	public void testToString() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent(INSTANCE_ID, JOB_NAME);
		assertEquals("JobInstanceEvent: id=1, version=null, Job=[FOOBAR]",
				jobInstanceEvent.toString());
	}
}
