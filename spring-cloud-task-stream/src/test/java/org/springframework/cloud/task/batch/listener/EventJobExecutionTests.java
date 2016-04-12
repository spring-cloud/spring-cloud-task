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

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Glenn Renfro.
 */
public class EventJobExecutionTests {

	private static final String JOB_NAME = "FOODJOB";
	private static final Long JOB_INSTANCE_ID = 1l;
	private static final Long JOB_EXECUTION_ID = 2l;
	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";

	private JobParameters jobParameters;
	private JobInstance jobInstance;

	@Before
	public void setup() {
		jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		jobParameters = new JobParameters();
	}

	@Test
	public void testBasic() {
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,  jobParameters, JOB_CONFIGURATION_NAME);
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(jobExecution);
		assertNotNull("jobInstance should not be null", jobExecutionEvent.getJobInstance());
		assertNotNull("jobParameters should not be null", jobExecutionEvent.getJobParameters());
		assertEquals("jobConfigurationName did not match expected", JOB_CONFIGURATION_NAME,
				jobExecutionEvent.getJobConfigurationName());

		assertEquals("jobParameters size did not match", 0, jobExecutionEvent.getJobParameters().getParameters().size());
		assertEquals("jobInstance name did not match", JOB_NAME, jobExecutionEvent.getJobInstance().getJobName());
		assertEquals("no step executions were expected", 0, jobExecutionEvent.getStepExecutions().size());
		assertEquals("exitStatus did not match expected", "UNKNOWN", jobExecutionEvent.getExitStatus().getExitCode());
	}

	@Test
	public void testJobParameters() {
		String[] JOB_PARAM_KEYS = { "A", "B", "C", "D" };
		Date testDate = new Date();
		JobParameter[] PARAMETERS = { new JobParameter("FOO", true), new JobParameter(1L, true),
				new JobParameter(1D, true), new JobParameter(testDate, false) };

		Map jobParamMap = new LinkedHashMap<String, JobParameter>();
		for (int paramCount = 0; paramCount < JOB_PARAM_KEYS.length; paramCount++) {
			jobParamMap.put(JOB_PARAM_KEYS[paramCount], PARAMETERS[paramCount]);
		}
		jobParameters = new JobParameters(jobParamMap);
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,  jobParameters, JOB_CONFIGURATION_NAME);
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(jobExecution);

		assertNotNull("Job Parameter A was expected", jobExecutionEvent.getJobParameters().getString("A"));
		assertNotNull("Job Parameter B was expected", jobExecutionEvent.getJobParameters().getLong("B"));
		assertNotNull("Job Parameter C was expected", jobExecutionEvent.getJobParameters().getDouble("C"));
		assertNotNull("Job Parameter D was expected", jobExecutionEvent.getJobParameters().getDate("D"));

		assertEquals("Job Parameter A value was not correct", "FOO", jobExecutionEvent.getJobParameters().getString("A"));
		assertEquals("Job Parameter B value was not correct", new Long(1), jobExecutionEvent.getJobParameters().getLong("B"));
		assertEquals("Job Parameter C value was not correct", new Double(1), jobExecutionEvent.getJobParameters().getDouble("C"));
		assertEquals("Job Parameter D value was not correct", testDate, jobExecutionEvent.getJobParameters().getDate("D"));
	}

	@Test
	public void testStepExecutions() {
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID,  jobParameters, JOB_CONFIGURATION_NAME);
		List<StepExecution> stepsExecutions = new ArrayList<>();
		stepsExecutions.add( new StepExecution("foo", jobExecution));
		stepsExecutions.add( new StepExecution("bar", jobExecution));
		stepsExecutions.add( new StepExecution("baz", jobExecution));
		jobExecution.addStepExecutions(stepsExecutions);

		JobExecutionEvent jobExecutionsEvent = new JobExecutionEvent(jobExecution);
		assertEquals("stepExecutions count is incorrect", 3, jobExecutionsEvent.getStepExecutions().size());
		Iterator<StepExecutionEvent> iter = jobExecutionsEvent.getStepExecutions().iterator();
		assertEquals("foo stepExecution is not present", "foo", iter.next().getStepName());
		assertEquals("bar stepExecution is not present", "bar", iter.next().getStepName());
		assertEquals("baz stepExecution is not present", "baz", iter.next().getStepName());

	}
}
