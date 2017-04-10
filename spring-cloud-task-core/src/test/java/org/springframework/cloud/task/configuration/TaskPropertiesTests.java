package org.springframework.cloud.task.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(Suite.class)
@SuiteClasses({
		TaskPropertiesTests.CloseContextEnableTest.class,
		TaskPropertiesTests.CloseContextEnabledTest.class,
		
})

@DirtiesContext
public class TaskPropertiesTests {	
	@Test
	public void test() {
	}
	
	@RunWith(SpringRunner.class)
	@SpringBootTest(classes={TaskPropertiesTests.Config.class}, properties = { "spring.cloud.task.closecontextEnable=false" })
	@DirtiesContext
	public static class CloseContextEnableTest extends TaskPropertiesTests {
	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes={TaskPropertiesTests.Config.class}, properties = { "spring.cloud.task.closecontextEnabled=false" })
	@DirtiesContext
	public static class CloseContextEnabledTest extends TaskPropertiesTests {}
	
	
	@Configuration
	@EnableTask
	public static class Config {
		@Bean
		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean() {
			return new TaskExecutionDaoFactoryBean();
		}
		@Bean
		public TaskRepository taskRepository(TaskExecutionDaoFactoryBean tefb) {
			return new SimpleTaskRepository(tefb);
		}
	}
}
	