/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.task.util;

import org.aspectj.lang.JoinPoint;
import org.springframework.cloud.task.configuration.TaskHandler;
import org.springframework.cloud.task.repository.LoggerTaskRepository;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the beans needed to test default task behavior.
 *
 * @author Glenn Renfro
 */
@Configuration
public class TestDefaultConfiguration {

	@Bean
	public TaskRepository taskRepository(){
		return new LoggerTaskRepository();
	}

	@Bean
	public TaskHandler taskHandler(){
		return new TaskHandler();
	}

	@Bean
	public JoinPoint joinPoint(){
		return new TestJoinPoint();
	}


}
