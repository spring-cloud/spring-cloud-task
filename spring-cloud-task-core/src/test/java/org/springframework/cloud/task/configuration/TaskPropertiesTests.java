package org.springframework.cloud.task.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


@RunWith(Suite.class)
@SuiteClasses({
		TaskPropertiesTests.CloseContextEnabledTest.class,
		
})

@DirtiesContext
public class TaskPropertiesTests {
	@Autowired
	TaskProperties taskProperties;

	@Test
	public void test() {
		assertThat(taskProperties.getClosecontextEnabled(), is(false));
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
