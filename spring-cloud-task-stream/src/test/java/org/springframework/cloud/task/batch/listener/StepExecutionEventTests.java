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

package org.springframework.cloud.task.batch.listener;

import java.util.Date;

import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.task.batch.listener.support.ExitStatus;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro
 */
public class StepExecutionEventTests {
	private static final String JOB_NAME = "FOO_JOB";
	private static final String STEP_NAME = "STEP_NAME";
	private static final Long JOB_INSTANCE_ID = 1l;
	private static final Long JOB_EXECUTION_ID = 2l;
	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";

	@Test
	public void testBasic(){
		StepExecution stepExecution = getBasicStepExecution();
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

	@Test
	public void testException() {
		RuntimeException exception = new RuntimeException("EXPECTED EXCEPTION");
		StepExecution stepExecution = getBasicStepExecution();
		stepExecution.addFailureException(exception);
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertEquals(1, stepExecutionEvent.getFailureExceptions().size());
		assertEquals(exception, stepExecution.getFailureExceptions().get(0));
	}

	@Test
	public void testGetSummary() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertEquals("StepExecutionEvent: id=null, version=null, name=STEP_NAME, status=STARTING,"
				+ " exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0,"
				+ " writeSkipCount=0, processSkipCount=0, commitCount=0, rollbackCount=0",
				stepExecutionEvent.getSummary());
	}

	@Test
	public void testHashCode() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent =
				new StepExecutionEvent(stepExecution);
		assertEquals("StepExecutionEvent: id=null, version=null, "
				+ "name=STEP_NAME, status=STARTING, exitStatus=EXECUTING, "
				+ "readCount=0, filterCount=0, writeCount=0 readSkipCount=0, "
				+ "writeSkipCount=0, processSkipCount=0, commitCount=0, "
				+ "rollbackCount=0, exitDescription=", stepExecutionEvent.toString());
	}

	@Test
	public void testToString() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertNotNull(stepExecutionEvent.hashCode());
	}

	@Test
	public void testEquals() {
		StepExecution stepExecution = getBasicStepExecution();
		stepExecution.setId(1L);
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertFalse(stepExecutionEvent.equals(getBasicStepExecution()));
		assertTrue(stepExecutionEvent.equals(stepExecution));
	}

	@Test
	public void testSettersGetters() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(getBasicStepExecution());
		Date date = new Date();
		stepExecutionEvent.setLastUpdated(date);
		assertEquals(date, stepExecutionEvent.getLastUpdated());

		stepExecutionEvent.setProcessSkipCount(55);
		assertEquals(55, stepExecutionEvent.getProcessSkipCount());

		stepExecutionEvent.setWriteSkipCount(47);
		assertEquals(47, stepExecutionEvent.getWriteSkipCount());

		stepExecutionEvent.setReadSkipCount(49);
		assertEquals(49, stepExecutionEvent.getReadSkipCount());

		assertEquals(0, stepExecutionEvent.getCommitCount());
		stepExecutionEvent.incrementCommitCount();
		assertEquals(1, stepExecutionEvent.getCommitCount());

		assertFalse(stepExecutionEvent.isTerminateOnly());
		stepExecutionEvent.setTerminateOnly();
		assertTrue(stepExecutionEvent.isTerminateOnly());

		stepExecutionEvent.setStepName("FOOBAR");
		assertEquals("FOOBAR", stepExecutionEvent.getStepName());

		stepExecutionEvent.setStartTime(date);
		assertEquals(date, stepExecutionEvent.getStartTime());

		assertEquals(0, stepExecutionEvent.getRollbackCount());
		stepExecutionEvent.setRollbackCount(33);
		assertEquals(33, stepExecutionEvent.getRollbackCount());

		stepExecutionEvent.setFilterCount(23);
		assertEquals(23, stepExecutionEvent.getFilterCount());

		stepExecutionEvent.setWriteCount(11);
		assertEquals(11,stepExecutionEvent.getWriteCount());

		stepExecutionEvent.setReadCount(12);
		assertEquals(12, stepExecutionEvent.getReadCount());

		stepExecutionEvent.setEndTime(date);
		assertEquals(date, stepExecutionEvent.getEndTime());

		stepExecutionEvent.setCommitCount(29);
		assertEquals(29, stepExecutionEvent.getCommitCount());
	}

	@Test
	public  void testExitStatus() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(getBasicStepExecution());
		final String EXIT_CODE = "1";
		final String EXIT_DESCRIPTION = "EXPECTED FAILURE";
		ExitStatus exitStatus = new ExitStatus();
		exitStatus.setExitCode(EXIT_CODE);
		exitStatus.setExitDescription(EXIT_DESCRIPTION);

		stepExecutionEvent.setExitStatus(exitStatus);
		ExitStatus actualExitStatus = stepExecutionEvent.getExitStatus();
		assertNotNull(actualExitStatus);
		assertEquals(exitStatus.getExitCode(), actualExitStatus.getExitCode());
		assertEquals(exitStatus.getExitDescription(), actualExitStatus.getExitDescription());
	}

	@Test
	public  void testBatchStatus() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(getBasicStepExecution());
		assertEquals(BatchStatus.STARTING, stepExecutionEvent.getStatus());
		stepExecutionEvent.setStatus(BatchStatus.ABANDONED);
		assertEquals(BatchStatus.ABANDONED, stepExecutionEvent.getStatus());
	}

	@Test
	public void testDefaultConstructor() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent();
		assertEquals(BatchStatus.STARTING, stepExecutionEvent.getStatus());
		assertNotNull(stepExecutionEvent.getExitStatus());
		assertEquals("EXECUTING", stepExecutionEvent.getExitStatus().getExitCode());
	}

	@Test
	public void testExecutionContext() {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("hello", "world");
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(getBasicStepExecution());
		assertNotNull(stepExecutionEvent.getExecutionContext());
		stepExecutionEvent.setExecutionContext(executionContext);
		assertEquals("world", stepExecutionEvent.getExecutionContext().getString("hello"));
	}

	private StepExecution getBasicStepExecution() {
		JobInstance jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,  jobParameters, JOB_CONFIGURATION_NAME);
		return new StepExecution(STEP_NAME, jobExecution);
	}
}
