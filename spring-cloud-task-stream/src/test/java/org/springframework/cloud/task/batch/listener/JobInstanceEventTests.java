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

import org.junit.Test;

import org.springframework.cloud.task.batch.listener.support.JobInstanceEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class JobInstanceEventTests {

	public static final long INSTANCE_ID = 1;

	public static final String JOB_NAME = "FOOBAR";

	@Test
	public void testConstructor() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent(INSTANCE_ID, JOB_NAME);
		assertThat(jobInstanceEvent.getInstanceId()).isEqualTo(INSTANCE_ID);
		assertThat(jobInstanceEvent.getJobName()).isEqualTo(JOB_NAME);
	}

	@Test
	public void testEmptyConstructor() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent();
		assertThat(jobInstanceEvent.getJobName()).isNull();
	}

	@Test
	public void testEmptyConstructorEmptyId() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent();
		jobInstanceEvent.getInstanceId();
	}

	@Test
	public void testToString() {
		JobInstanceEvent jobInstanceEvent = new JobInstanceEvent(INSTANCE_ID, JOB_NAME);
		assertThat(jobInstanceEvent.toString())
				.isEqualTo("JobInstanceEvent: id=1, version=null, Job=[FOOBAR]");
	}

}
