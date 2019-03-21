/*
 * Copyright 2016 the original author or authors.
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

package io.spring;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TaskProcessorApplication.class)
public class TaskProcessorApplicationTests {

	private static final String DEFAULT_PAYLOAD = "hello";

	@Autowired
	@Bindings(TaskProcessor.class)
	protected Processor channels;

	@Autowired
	protected MessageCollector collector;

		@Test
		public void test() throws InterruptedException{
			channels.input().send(new GenericMessage<Object>(DEFAULT_PAYLOAD));
			Map<String, String> properties = new HashMap();
			properties.put("payload", DEFAULT_PAYLOAD);
			TaskLaunchRequest expectedRequest = new TaskLaunchRequest("maven://org.springframework.cloud.task.app:"
					+ "timestamp-task:jar:1.0.0.BUILD-SNAPSHOT", null, properties, null);
			assertThat(collector.forChannel(channels.output()), receivesPayloadThat(is(expectedRequest)));
		}

}
