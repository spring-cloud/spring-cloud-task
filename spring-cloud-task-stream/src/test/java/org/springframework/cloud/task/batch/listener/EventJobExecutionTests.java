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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.cloud.stream.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro.
 */
public class EventJobExecutionTests {

	private static final String JOB_NAME = "FOODJOB";
	private static final Long JOB_INSTANCE_ID = 1l;
	private static final Long JOB_EXECUTION_ID = 2l;
	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";
	private static final String[] LISTENER_BEAN_NAMES = {BatchEventAutoConfiguration.JOB_EXECUTION_EVENTS_LISTENER,
			BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER, BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER,
			BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER, BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER,
			BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER, BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER};

	private JobParameters jobParameters;
	private JobInstance jobInstance;

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	@Before
	public void setup() {
		jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		jobParameters = new JobParameters();
	}

	@Test
	public void testBasic() {
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID, jobParameters, JOB_CONFIGURATION_NAME);
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
		String[] JOB_PARAM_KEYS = {"A", "B", "C", "D"};
		Date testDate = new Date();
		JobParameter[] PARAMETERS = {new JobParameter("FOO", true), new JobParameter(1L, true),
				new JobParameter(1D, true), new JobParameter(testDate, false)};

		Map jobParamMap = new LinkedHashMap<>();
		for (int paramCount = 0; paramCount < JOB_PARAM_KEYS.length; paramCount++) {
			jobParamMap.put(JOB_PARAM_KEYS[paramCount], PARAMETERS[paramCount]);
		}
		jobParameters = new JobParameters(jobParamMap);
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID, jobParameters, JOB_CONFIGURATION_NAME);
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
		JobExecution jobExecution = new JobExecution(jobInstance, JOB_EXECUTION_ID, jobParameters, JOB_CONFIGURATION_NAME);
		List<StepExecution> stepsExecutions = new ArrayList<>();
		stepsExecutions.add(new StepExecution("foo", jobExecution));
		stepsExecutions.add(new StepExecution("bar", jobExecution));
		stepsExecutions.add(new StepExecution("baz", jobExecution));
		jobExecution.addStepExecutions(stepsExecutions);

		JobExecutionEvent jobExecutionsEvent = new JobExecutionEvent(jobExecution);
		assertEquals("stepExecutions count is incorrect", 3, jobExecutionsEvent.getStepExecutions().size());
		Iterator<StepExecutionEvent> iter = jobExecutionsEvent.getStepExecutions().iterator();
		assertEquals("foo stepExecution is not present", "foo", iter.next().getStepName());
		assertEquals("bar stepExecution is not present", "bar", iter.next().getStepName());
		assertEquals("baz stepExecution is not present", "baz", iter.next().getStepName());
	}

	@Test
	public void testDefaultConfiguration() {
		testDisabledConfiguration(null, null);
	}

	@Test
	public void testDisabledJobExecutionListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.job-execution.enabled",
				BatchEventAutoConfiguration.JOB_EXECUTION_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledStepExecutionListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.step-execution.enabled",
				BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledChunkListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.chunk.enabled",
				BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledItemReadListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.item-read.enabled",
				BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledItemWriteListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.item-write.enabled",
				BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledItemProcessListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.item-process.enabled",
				BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER);
	}

	@Test
	public void testDisabledSkipEventListener() {
		testDisabledConfiguration("spring.cloud.task.batch.events.skip.enabled",
				BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER);
	}

	public void testDisabledConfiguration(String property, String disabledListener) {
		boolean exceptionThrown = false;
		String disabledPropertyArg = (property != null) ? "--" + property + "=false" : "";
		ConfigurableApplicationContext applicationContext =
				SpringApplication.run(new Object[]{BatchEventAutoConfiguration.JobExecutionListenerConfiguration.class,
								EventJobExecutionConfiguration.class,
								PropertyPlaceholderAutoConfiguration.class,
								TestSupportBinderAutoConfiguration.class},
						new String[]{"--spring.cloud.task.closecontext.enable=false",
								"--spring.main.web-environment=false",
								disabledPropertyArg});

		for (String beanName : LISTENER_BEAN_NAMES) {
			if (disabledListener != null && disabledListener.equals(beanName)) {
				try {
					applicationContext.getBean(disabledListener);
				}
				catch (NoSuchBeanDefinitionException nsbde) {
					exceptionThrown = true;
				}
				assertTrue(String.format("Did not expect %s bean in context", beanName), exceptionThrown);
			}
			else {
				applicationContext.getBean(beanName);
			}
		}
		applicationContext.getBean(BatchEventAutoConfiguration.BatchEventsChannels.class);
	}


	@Configuration
	@EnableTask
	public static class EventJobExecutionConfiguration {
	}
}
