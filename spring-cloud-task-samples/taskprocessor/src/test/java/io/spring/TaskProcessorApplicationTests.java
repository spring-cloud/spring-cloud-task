package io.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

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
			Map properties = new HashMap<String, String>();
			properties.put("payload", DEFAULT_PAYLOAD);
			properties.put("server.port", "0");
			TaskLaunchRequest expectedRequest = new TaskLaunchRequest("timestamp-task",
					"io.spring", "1.0.0.BUILD-SNAPSHOT", "jar", null, properties);
			assertThat(collector.forChannel(channels.output()), receivesPayloadThat(is(expectedRequest)));
		}

}
