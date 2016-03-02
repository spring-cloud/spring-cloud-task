package org.springframework.cloud.task.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.stream.annotation.Bindings;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.task.launcher.configuration.TaskConfiguration;
import org.springframework.cloud.task.launcher.util.TaskLauncherSinkApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.support.GenericMessage;

public class TaskLauncherSinkTests {

	private final static String DEFAULT_STATUS = "test_status";

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testTask");
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
	public void testSuccess() {
		context.register(TaskLauncherSinkApplication.class, TaskConfiguration.class);
		context.refresh();
		this.sink = context.getBeansOfType(Sink.class).values().iterator().next();
		assertNotNull(this.sink.input());

		TaskConfiguration.TestTaskLauncher testTaskLauncher =
				 context.getBean(TaskConfiguration.TestTaskLauncher.class);

		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest("timestamp-task",
				"org.springframework.cloud.task.module","1.0.0.BUILD-SNAPSHOT", "jar",
				"exec", properties);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
		this.sink.input().send(message);
		assertEquals(LaunchState.complete, testTaskLauncher.status(DEFAULT_STATUS).getState());
	}

	@Test
	public void testNoRun() {
		context.register(TaskLauncherSinkApplication.class, TaskConfiguration.class);
		context.refresh();
		this.sink = context.getBean(Sink.class);
		assertNotNull(this.sink.input());

		TaskConfiguration.TestTaskLauncher testTaskLauncher =
				 context.getBean(TaskConfiguration.TestTaskLauncher.class);

		assertEquals(LaunchState.unknown, testTaskLauncher.status(DEFAULT_STATUS).getState());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoTaskLauncher() {
		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", "0");
		TaskLaunchRequest request = new TaskLaunchRequest("timestamp-task",
				"org.springframework.cloud.task.module","1.0.0.BUILD-SNAPSHOT", "jar",
				"exec", properties);
		GenericMessage<TaskLaunchRequest> message = new GenericMessage<>(request);
		TaskLauncherSink sink = new TaskLauncherSink();
		sink.taskLauncherSink(message);
	}
}
