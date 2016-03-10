package io.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import io.spring.configuration.TaskSinkConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.cloud.task.launcher.TaskLauncherSink;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Glenn Renfro
 */
public class TaskSinkApplicationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.setId("sinkTest");
	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	@Autowired
	@Bindings(TaskLauncherSink.class)
	private Sink sink;

	@Test
	public void testLaunch() {
		context.register(TaskSinkApplication.class, TaskSinkConfiguration.class);
		context.refresh();
		this.sink = context.getBeansOfType(Sink.class).values().iterator().next();
		assertNotNull(this.sink.input());

		TaskSinkConfiguration.TestTaskLauncher testTaskLauncher =
				(TaskSinkConfiguration.TestTaskLauncher) context.getBean(TaskSinkConfiguration.TestTaskLauncher.class);

		Map<String, String> properties = new HashMap<String,String>();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest("timestamp-task",
				"org.springframework.cloud.task.module","1.0.0.BUILD-SNAPSHOT", "jar",
				"exec", properties);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<TaskLaunchRequest>(request);
		this.sink.input().send(message);
		assertEquals(LaunchState.complete, testTaskLauncher.status("TESTSTATUS").getState());
	}
}
