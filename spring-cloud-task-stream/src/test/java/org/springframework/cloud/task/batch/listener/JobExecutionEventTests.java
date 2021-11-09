/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.cloud.task.batch.listener.support.JobInstanceEvent;
import org.springframework.cloud.task.batch.listener.support.StepExecutionEvent;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro.
 * @author Ali Shahbour
 */
public class JobExecutionEventTests {

	private static final String JOB_NAME = "FOODJOB";

	private static final Long JOB_INSTANCE_ID = 1L;

	private static final Long JOB_EXECUTION_ID = 2L;

	private static final String JOB_CONFIGURATION_NAME = "FOO_JOB_CONFIG";

	private static final String[] LISTENER_BEAN_NAMES = {
		BatchEventAutoConfiguration.JOB_EXECUTION_EVENTS_LISTENER,
		BatchEventAutoConfiguration.STEP_EXECUTION_EVENTS_LISTENER,
		BatchEventAutoConfiguration.CHUNK_EVENTS_LISTENER,
		BatchEventAutoConfiguration.ITEM_READ_EVENTS_LISTENER,
		BatchEventAutoConfiguration.ITEM_WRITE_EVENTS_LISTENER,
		BatchEventAutoConfiguration.ITEM_PROCESS_EVENTS_LISTENER,
		BatchEventAutoConfiguration.SKIP_EVENTS_LISTENER};

	private JobParameters jobParameters;

	private JobInstance jobInstance;

	//
	@BeforeEach
	public void setup() {
		this.jobInstance = new JobInstance(JOB_INSTANCE_ID, JOB_NAME);
		this.jobParameters = new JobParameters();
	}

	@Test
	public void testBasic() {
		JobExecution jobExecution;
		jobExecution = new JobExecution(this.jobInstance, JOB_EXECUTION_ID,
			this.jobParameters, JOB_CONFIGURATION_NAME);
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(jobExecution);
		assertThat(jobExecutionEvent.getJobInstance())
			.as("jobInstance should not be null").isNotNull();
		assertThat(jobExecutionEvent.getJobParameters())
			.as("jobParameters should not be null").isNotNull();
		assertThat(jobExecutionEvent.getJobConfigurationName())
			.as("jobConfigurationName did not match expected")
			.isEqualTo(JOB_CONFIGURATION_NAME);

		assertThat(jobExecutionEvent.getJobParameters().getParameters().size())
			.as("jobParameters size did not match").isEqualTo(0);
		assertThat(jobExecutionEvent.getJobInstance().getJobName())
			.as("jobInstance name did not match").isEqualTo(JOB_NAME);
		assertThat(jobExecutionEvent.getStepExecutions().size())
			.as("no step executions were expected").isEqualTo(0);
		assertThat(jobExecutionEvent.getExitStatus().getExitCode())
			.as("exitStatus did not match expected").isEqualTo("UNKNOWN");
	}

	@Test
	public void testJobParameters() {
		String[] JOB_PARAM_KEYS = {"A", "B", "C", "D"};
		Date testDate = new Date();
		JobParameter[] PARAMETERS = {new JobParameter("FOO", true),
			new JobParameter(1L, true), new JobParameter(1D, true),
			new JobParameter(testDate, false)};

		Map<String, JobParameter> jobParamMap = new LinkedHashMap<>();
		for (int paramCount = 0; paramCount < JOB_PARAM_KEYS.length; paramCount++) {
			jobParamMap.put(JOB_PARAM_KEYS[paramCount], PARAMETERS[paramCount]);
		}
		this.jobParameters = new JobParameters(jobParamMap);
		JobExecution jobExecution;
		jobExecution = new JobExecution(this.jobInstance, JOB_EXECUTION_ID,
			this.jobParameters, JOB_CONFIGURATION_NAME);
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(jobExecution);

		assertThat(jobExecutionEvent.getJobParameters().getString("A"))
			.as("Job Parameter A was expected").isNotNull();
		assertThat(jobExecutionEvent.getJobParameters().getLong("B"))
			.as("Job Parameter B was expected").isNotNull();
		assertThat(jobExecutionEvent.getJobParameters().getDouble("C"))
			.as("Job Parameter C was expected").isNotNull();
		assertThat(jobExecutionEvent.getJobParameters().getDate("D"))
			.as("Job Parameter D was expected").isNotNull();

		assertThat(jobExecutionEvent.getJobParameters().getString("A"))
			.as("Job Parameter A value was not correct").isEqualTo("FOO");
		assertThat(jobExecutionEvent.getJobParameters().getLong("B"))
			.as("Job Parameter B value was not correct").isEqualTo(new Long(1));
		assertThat(jobExecutionEvent.getJobParameters().getDouble("C"))
			.as("Job Parameter C value was not correct").isEqualTo(new Double(1));
		assertThat(jobExecutionEvent.getJobParameters().getDate("D"))
			.as("Job Parameter D value was not correct").isEqualTo(testDate);
	}

	@Test
	public void testStepExecutions() {
		JobExecution jobExecution;
		jobExecution = new JobExecution(this.jobInstance, JOB_EXECUTION_ID,
			this.jobParameters, JOB_CONFIGURATION_NAME);
		List<StepExecution> stepsExecutions = new ArrayList<>();
		stepsExecutions.add(new StepExecution("foo", jobExecution));
		stepsExecutions.add(new StepExecution("bar", jobExecution));
		stepsExecutions.add(new StepExecution("baz", jobExecution));
		jobExecution.addStepExecutions(stepsExecutions);

		JobExecutionEvent jobExecutionsEvent = new JobExecutionEvent(jobExecution);
		assertThat(jobExecutionsEvent.getStepExecutions().size())
			.as("stepExecutions count is incorrect").isEqualTo(3);
		Iterator<StepExecutionEvent> iter = jobExecutionsEvent.getStepExecutions()
			.iterator();
		assertThat(iter.next().getStepName()).as("foo stepExecution is not present")
			.isEqualTo("foo");
		assertThat(iter.next().getStepName()).as("bar stepExecution is not present")
			.isEqualTo("bar");
		assertThat(iter.next().getStepName()).as("baz stepExecution is not present")
			.isEqualTo("baz");
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

	@Test
	public void testDefaultConstructor() {
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getExitStatus().getExitCode()).isEqualTo("UNKNOWN");
	}

	@Test
	public void testFailureExceptions() {
		final String EXCEPTION_MESSAGE = "TEST EXCEPTION";
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getFailureExceptions().size()).isEqualTo(0);
		jobExecutionEvent
				.addFailureException(new IllegalStateException(EXCEPTION_MESSAGE));
		assertThat(jobExecutionEvent.getFailureExceptions().size()).isEqualTo(1);
		assertThat(jobExecutionEvent.getAllFailureExceptions().size()).isEqualTo(1);
		assertThat(EXCEPTION_MESSAGE)
				.isEqualTo(jobExecutionEvent.getFailureExceptions().get(0).getMessage());
		assertThat(EXCEPTION_MESSAGE).isEqualTo(
				jobExecutionEvent.getAllFailureExceptions().get(0).getMessage());
	}

	@Test
	public void testToString() {
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.toString().startsWith("JobExecutionEvent:"))
				.isTrue();
	}

	@Test
	public void testGetterSetters() {
		Date date = new Date();
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		jobExecutionEvent.setLastUpdated(date);
		assertThat(jobExecutionEvent.getLastUpdated()).isEqualTo(date);
		jobExecutionEvent.setCreateTime(date);
		assertThat(jobExecutionEvent.getCreateTime()).isEqualTo(date);
		jobExecutionEvent.setEndTime(date);
		assertThat(jobExecutionEvent.getEndTime()).isEqualTo(date);
		jobExecutionEvent.setStartTime(date);
		assertThat(jobExecutionEvent.getStartTime()).isEqualTo(date);
	}

	@Test
	public void testExitStatus() {
		final String EXIT_CODE = "KNOWN";
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getExitStatus().getExitCode()).isEqualTo("UNKNOWN");
		org.springframework.cloud.task.batch.listener.support.ExitStatus expectedExitStatus;
		expectedExitStatus = new org.springframework.cloud.task.batch.listener.support.ExitStatus();
		expectedExitStatus.setExitCode(EXIT_CODE);
		jobExecutionEvent.setExitStatus(expectedExitStatus);
		assertThat(jobExecutionEvent.getExitStatus().getExitCode()).isEqualTo(EXIT_CODE);
	}

	@Test
	public void testJobInstance() {
		final String JOB_NAME = "KNOWN";
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getJobInstance()).isNull();
		assertThat(jobExecutionEvent.getJobId()).isNull();
		JobInstanceEvent expectedJobInstanceEvent = new JobInstanceEvent(1L, JOB_NAME);
		jobExecutionEvent.setJobInstance(expectedJobInstanceEvent);
		assertThat(jobExecutionEvent.getJobInstance().getJobName())
				.isEqualTo(expectedJobInstanceEvent.getJobName());
		assertThat(jobExecutionEvent.getJobId())
				.isEqualTo(expectedJobInstanceEvent.getId());
	}

	@Test
	public void testExecutionContext() {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("hello", "world");
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getExecutionContext()).isNotNull();
		jobExecutionEvent.setExecutionContext(executionContext);
		assertThat(jobExecutionEvent.getExecutionContext().getString("hello"))
				.isEqualTo("world");
	}

	@Test
	public void testBatchStatus() {
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getStatus()).isEqualTo(BatchStatus.STARTING);
		jobExecutionEvent.setStatus(BatchStatus.ABANDONED);
		assertThat(jobExecutionEvent.getStatus()).isEqualTo(BatchStatus.ABANDONED);
	}

	@Test
	public void testUpgradeBatchStatus() {
		JobExecutionEvent jobExecutionEvent = new JobExecutionEvent();
		assertThat(jobExecutionEvent.getStatus()).isEqualTo(BatchStatus.STARTING);
		jobExecutionEvent.upgradeStatus(BatchStatus.FAILED);
		assertThat(jobExecutionEvent.getStatus()).isEqualTo(BatchStatus.FAILED);
		jobExecutionEvent.upgradeStatus(BatchStatus.COMPLETED);
		assertThat(jobExecutionEvent.getStatus()).isEqualTo(BatchStatus.FAILED);
	}

	@Test
	public void testOrderConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(
						PropertyPlaceholderAutoConfiguration.class,
						SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
				.withUserConfiguration(
						BatchEventAutoConfiguration.JobExecutionListenerConfiguration.class,
					BatchEventTestApplication.class)
				.withPropertyValues("--spring.cloud.task.closecontext_enabled=false",
						"--spring.main.web-environment=false",
						"--spring.cloud.task.batch.events.chunk-order=5",
						"--spring.cloud.task.batch.events.item-process-order=5",
						"--spring.cloud.task.batch.events.item-read-order=5",
						"--spring.cloud.task.batch.events.item-write-order=5",
						"--spring.cloud.task.batch.events.job-execution-order=5",
						"--spring.cloud.task.batch.events.skip-order=5",
						"--spring.cloud.task.batch.events.step-execution-order=5");
		applicationContextRunner.run((context) -> {
			for (String beanName : LISTENER_BEAN_NAMES) {
				Ordered ordered = (Ordered) context.getBean(beanName);
				assertThat(5).as("Expected order value of 5 for " + beanName)
						.isEqualTo(ordered.getOrder());
			}

		});
	}

	@Test
	public void singleStepBatchJobSkip() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration.class,
				SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
			.withUserConfiguration(
				BatchEventAutoConfiguration.JobExecutionListenerConfiguration.class,
				BatchEventTestApplication.class)
			.withPropertyValues("--spring.cloud.task.closecontext_enabled=false",
				"--spring.main.web-environment=false", "spring.batch.job.jobName=FOO");
		applicationContextRunner.run((context) -> {
			NoSuchBeanDefinitionException exception = Assertions.assertThrows(NoSuchBeanDefinitionException.class, () -> {
				context.getBean("jobExecutionEventsListener");
			});
			assertThat(exception.getMessage()).contains(
					String.format("No bean named 'jobExecutionEventsListener' available"));
		});
	}

	private void testDisabledConfiguration(String property, String disabledListener) {
		String disabledPropertyArg = (property != null) ? "--" + property + "=false" : "";
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration.class,
				SimpleTaskAutoConfiguration.class, SingleTaskConfiguration.class))
			.withUserConfiguration(
				BatchEventAutoConfiguration.JobExecutionListenerConfiguration.class,
				BatchEventTestApplication.class)
			.withPropertyValues("--spring.cloud.task.closecontext_enabled=false",
				"--spring.main.web-environment=false", disabledPropertyArg);
		applicationContextRunner.run((context) -> {
			boolean exceptionThrown = false;
			for (String beanName : LISTENER_BEAN_NAMES) {
				if (disabledListener != null && disabledListener.equals(beanName)) {
					try {
						context.getBean(disabledListener);
					}
					catch (NoSuchBeanDefinitionException nsbde) {
						exceptionThrown = true;
					}
					assertThat(exceptionThrown).as(
							String.format("Did not expect %s bean in context", beanName))
						.isTrue();
				}
				else {
					context.getBean(beanName);
				}
			}
		});
	}

	@SpringBootApplication
	public static class BatchEventTestApplication {
	}
}
