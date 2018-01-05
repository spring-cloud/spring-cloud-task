/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TaskProcessorApplication.class)
public class TaskProcessorApplicationTests {

	private static final String DEFAULT_PAYLOAD = "hello";

	@Autowired
	protected Processor channels;

	@Autowired
	protected MessageCollector collector;

	private ObjectMapper mapper = new ObjectMapper();

		@Test
		public void test() throws InterruptedException, IOException {
			channels.input().send(new GenericMessage<Object>(DEFAULT_PAYLOAD));
			Map<String, String> properties = new HashMap();
			properties.put("payload", DEFAULT_PAYLOAD);
			TaskLaunchRequest expectedRequest = new TaskLaunchRequest(
					"maven://org.springframework.cloud.task.app:"
					+ "timestamp-task:jar:1.0.1.RELEASE", null, properties,
					null, null);
			Message<String> result = (Message<String>)collector.forChannel(channels.output()).take();
			TaskLaunchRequest tlq = mapper.readValue(result.getPayload(),TaskLaunchRequest.class);
			assertThat(tlq, is(expectedRequest));
		}

}
