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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.task.batch.listener.support.ExitStatus;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class StepExecutionEventTests {

	private static final String JOB_NAME = "FOO_JOB";

	private static final String STEP_NAME = "STEP_NAME";

	private static final Long JOB_INSTANCE_ID = 1L;

	private static final Long JOB_EXECUTION_ID = 2L;

	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";

	@Test
	public void testBasic() {
		StepExecution stepExecution = getBasicStepExecution();
		stepExecution.setCommitCount(1);
		stepExecution.setReadCount(2);
		stepExecution.setWriteCount(3);
		stepExecution.setReadSkipCount(4);
		stepExecution.setWriteSkipCount(5);

		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.getStepName())
				.as("stepName result was not as expected").isEqualTo(STEP_NAME);
		assertThat(stepExecutionEvent.getStartTime())
				.as("startTime result was not as expected")
				.isEqualTo(stepExecution.getStartTime());
		assertThat(stepExecutionEvent.getEndTime())
				.as("endTime result was not as expected")
				.isEqualTo(stepExecution.getEndTime());
		assertThat(stepExecutionEvent.getLastUpdated())
				.as("lastUpdated result was not as expected")
				.isEqualTo(stepExecution.getLastUpdated());
		assertThat(stepExecutionEvent.getCommitCount())
				.as("commitCount result was not as expected")
				.isEqualTo(stepExecution.getCommitCount());
		assertThat(stepExecutionEvent.getReadCount())
				.as("readCount result was not as expected")
				.isEqualTo(stepExecution.getReadCount());
		assertThat(stepExecutionEvent.getReadSkipCount())
				.as("readSkipCount result was not as expected")
				.isEqualTo(stepExecution.getReadSkipCount());
		assertThat(stepExecutionEvent.getWriteCount())
				.as("writeCount result was not as expected")
				.isEqualTo(stepExecution.getWriteCount());
		assertThat(stepExecutionEvent.getWriteSkipCount())
				.as("writeSkipCount result was not as expected")
				.isEqualTo(stepExecution.getWriteSkipCount());
		assertThat(stepExecutionEvent.getSkipCount())
				.as("skipCount result was not as expected")
				.isEqualTo(stepExecution.getSkipCount());
	}

	@Test
	public void testException() {
		RuntimeException exception = new RuntimeException("EXPECTED EXCEPTION");
		StepExecution stepExecution = getBasicStepExecution();
		stepExecution.addFailureException(exception);
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.getFailureExceptions().size()).isEqualTo(1);
		assertThat(stepExecution.getFailureExceptions().get(0)).isEqualTo(exception);
	}

	@Test
	public void testGetSummary() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.getSummary()).isEqualTo(
				"StepExecutionEvent: id=null, version=null, name=STEP_NAME, status=STARTING,"
						+ " exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0,"
						+ " writeSkipCount=0, processSkipCount=0, commitCount=0, rollbackCount=0");
	}

	@Test
	public void testHashCode() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.toString())
				.isEqualTo("StepExecutionEvent: id=null, version=null, "
						+ "name=STEP_NAME, status=STARTING, exitStatus=EXECUTING, "
						+ "readCount=0, filterCount=0, writeCount=0 readSkipCount=0, "
						+ "writeSkipCount=0, processSkipCount=0, commitCount=0, "
						+ "rollbackCount=0, exitDescription=");
	}

	@Test
	public void testToString() {
		StepExecution stepExecution = getBasicStepExecution();
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.hashCode()).isNotNull();
	}

	@Test
	public void testEquals() {
		StepExecution stepExecution = getBasicStepExecution();
		stepExecution.setId(1L);
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(stepExecution);
		assertThat(stepExecutionEvent.equals(getBasicStepExecution())).isFalse();
		assertThat(stepExecutionEvent.equals(stepExecution)).isTrue();
	}

	@Test
	public void testSettersGetters() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(
				getBasicStepExecution());
		Date date = new Date();
		stepExecutionEvent.setLastUpdated(date);
		assertThat(stepExecutionEvent.getLastUpdated()).isEqualTo(date);

		stepExecutionEvent.setProcessSkipCount(55);
		assertThat(stepExecutionEvent.getProcessSkipCount()).isEqualTo(55);

		stepExecutionEvent.setWriteSkipCount(47);
		assertThat(stepExecutionEvent.getWriteSkipCount()).isEqualTo(47);

		stepExecutionEvent.setReadSkipCount(49);
		assertThat(stepExecutionEvent.getReadSkipCount()).isEqualTo(49);

		assertThat(stepExecutionEvent.getCommitCount()).isEqualTo(0);
		stepExecutionEvent.incrementCommitCount();
		assertThat(stepExecutionEvent.getCommitCount()).isEqualTo(1);

		assertThat(stepExecutionEvent.isTerminateOnly()).isFalse();
		stepExecutionEvent.setTerminateOnly();
		assertThat(stepExecutionEvent.isTerminateOnly()).isTrue();

		stepExecutionEvent.setStepName("FOOBAR");
		assertThat(stepExecutionEvent.getStepName()).isEqualTo("FOOBAR");

		stepExecutionEvent.setStartTime(date);
		assertThat(stepExecutionEvent.getStartTime()).isEqualTo(date);

		assertThat(stepExecutionEvent.getRollbackCount()).isEqualTo(0);
		stepExecutionEvent.setRollbackCount(33);
		assertThat(stepExecutionEvent.getRollbackCount()).isEqualTo(33);

		stepExecutionEvent.setFilterCount(23);
		assertThat(stepExecutionEvent.getFilterCount()).isEqualTo(23);

		stepExecutionEvent.setWriteCount(11);
		assertThat(stepExecutionEvent.getWriteCount()).isEqualTo(11);

		stepExecutionEvent.setReadCount(12);
		assertThat(stepExecutionEvent.getReadCount()).isEqualTo(12);

		stepExecutionEvent.setEndTime(date);
		assertThat(stepExecutionEvent.getEndTime()).isEqualTo(date);

		stepExecutionEvent.setCommitCount(29);
		assertThat(stepExecutionEvent.getCommitCount()).isEqualTo(29);
	}

	@Test
	public void testExitStatus() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(
				getBasicStepExecution());
		final String EXIT_CODE = "1";
		final String EXIT_DESCRIPTION = "EXPECTED FAILURE";
		ExitStatus exitStatus = new ExitStatus();
		exitStatus.setExitCode(EXIT_CODE);
		exitStatus.setExitDescription(EXIT_DESCRIPTION);

		stepExecutionEvent.setExitStatus(exitStatus);
		ExitStatus actualExitStatus = stepExecutionEvent.getExitStatus();
		assertThat(actualExitStatus).isNotNull();
		assertThat(actualExitStatus.getExitCode()).isEqualTo(exitStatus.getExitCode());
		assertThat(actualExitStatus.getExitDescription())
				.isEqualTo(exitStatus.getExitDescription());
	}

	@Test
	public void testBatchStatus() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(
				getBasicStepExecution());
		assertThat(stepExecutionEvent.getStatus()).isEqualTo(BatchStatus.STARTING);
		stepExecutionEvent.setStatus(BatchStatus.ABANDONED);
		assertThat(stepExecutionEvent.getStatus()).isEqualTo(BatchStatus.ABANDONED);
	}

	@Test
	public void testDefaultConstructor() {
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent();
		assertThat(stepExecutionEvent.getStatus()).isEqualTo(BatchStatus.STARTING);
		assertThat(stepExecutionEvent.getExitStatus()).isNotNull();
		assertThat(stepExecutionEvent.getExitStatus().getExitCode())
				.isEqualTo("EXECUTING");
	}

	@Test
	public void testExecutionContext() {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("hello", "world");
		StepExecutionEvent stepExecutionEvent = new StepExecutionEvent(
				getBasicStepExecution());
		assertThat(stepExecutionEvent.getExecutionContext()).isNotNull();
		stepExecutionEvent.setExecutionContext(executionContext);
		assertThat(stepExecutionEvent.getExecutionContext().getString("hello"))
				.isEqualTo("world");
	}

	private StepExecution getBasicStepExecution() {
		JobInstance jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,
				jobParameters, JOB_CONFIGURATION_NAME);
		return new StepExecution(STEP_NAME, jobExecution);
	}

}
