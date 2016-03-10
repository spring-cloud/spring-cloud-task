package io.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.launcher.annotation.EnableTaskLauncher;

/**
 * A task sink sample application for launching tasks.
 *
 * @author Glenn Renfro
 */
@SpringBootApplication
@EnableTaskLauncher
public class TaskSinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskSinkApplication.class, args);
	}
}
