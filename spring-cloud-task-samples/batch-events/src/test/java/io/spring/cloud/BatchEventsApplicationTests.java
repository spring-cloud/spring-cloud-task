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

package io.spring.cloud;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.BDDAssertions;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.test.junit.rabbit.RabbitTestSupport;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.batch.listener.support.JobExecutionEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchEventsApplicationTests {

	@ClassRule
	public static RabbitTestSupport rabbitTestSupport = new RabbitTestSupport();

	// Count for two job execution events per task
	static CountDownLatch jobExecutionLatch = new CountDownLatch(2);

	@Test
	public void testExecution() throws Exception {
		SpringApplication
			.run(JobExecutionListenerBinding.class, "--spring.main.web-environment=false");
		SpringApplication.run(BatchEventsApplication.class, "--server.port=0",
			"--spring.cloud.stream.bindings.output.producer.requiredGroups=testgroup",
			"--spring.jmx.default-domain=fakedomain",
			"--spring.main.webEnvironment=false");
		assertThat(jobExecutionLatch.await(60, TimeUnit.SECONDS))
			.as("The latch did not count down to zero before timeout").isTrue();
	}

	@EnableBinding(Sink.class)
	@PropertySource("classpath:io/spring/task/listener/job-listener-sink-channel.properties")
	@Configuration
	public static class JobExecutionListenerBinding {

		@StreamListener(Sink.INPUT)
		public void receive(JobExecutionEvent execution) {
			BDDAssertions.then(execution.getJobInstance().getJobName())
				.isEqualTo("job").as("Job name should be job");
			jobExecutionLatch.countDown();
		}
	}

}
