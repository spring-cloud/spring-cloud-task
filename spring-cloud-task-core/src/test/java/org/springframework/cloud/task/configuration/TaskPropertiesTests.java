/*
 * Copyright 2015-2017 the original author or authors.
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
		TaskPropertiesTests.CloseContextEnableTest.class,
		TaskPropertiesTests.CloseContextEnabledTest.class,
		
})

@DirtiesContext
public class TaskPropertiesTests {
	@Autowired
	private TaskProperties taskProperties;

	@Test
	public void test() {
		assertThat(taskProperties.getClosecontextEnabled(), is(false));
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
