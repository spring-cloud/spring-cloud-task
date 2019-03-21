/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;

import static org.junit.Assert.assertEquals;

/**
 * @author Glenn Renfro
 */
public class EventStepExecutionTests {
	private static final String JOB_NAME = "FOO_JOB";
	private static final String STEP_NAME = "STEP_NAME";
	private static final Long JOB_INSTANCE_ID = 1l;
	private static final Long JOB_EXECUTION_ID = 2l;
	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";

	@Test
	public void testBasic(){
		JobInstance jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,  jobParameters, JOB_CONFIGURATION_NAME);

		StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution);
		stepExecution.setCommitCount(1);
		stepExecution.setReadCount(2);
		stepExecution.setWriteCount(3);
		stepExecution.setReadSkipCount(4);
		stepExecution.setWriteSkipCount(5);

		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertEquals("stepName result was not as expected", STEP_NAME, stepExecutionEvent.getStepName());
		assertEquals("startTime result was not as expected", stepExecution.getStartTime(), stepExecutionEvent.getStartTime());
		assertEquals("endTime result was not as expected", stepExecution.getEndTime(), stepExecutionEvent.getEndTime());
		assertEquals("lastUpdated result was not as expected", stepExecution.getLastUpdated(), stepExecutionEvent.getLastUpdated());
		assertEquals("commitCount result was not as expected", stepExecution.getCommitCount(), stepExecutionEvent.getCommitCount());
		assertEquals("readCount result was not as expected", stepExecution.getReadCount(), stepExecutionEvent.getReadCount());
		assertEquals("readSkipCount result was not as expected", stepExecution.getReadSkipCount(), stepExecutionEvent.getReadSkipCount());
		assertEquals("writeCount result was not as expected", stepExecution.getWriteCount(), stepExecutionEvent.getWriteCount());
		assertEquals("writeSkipCount result was not as expected", stepExecution.getWriteSkipCount(), stepExecutionEvent.getWriteSkipCount());
		assertEquals("skipCount result was not as expected", stepExecution.getSkipCount(), stepExecutionEvent.getSkipCount());
	}
}
