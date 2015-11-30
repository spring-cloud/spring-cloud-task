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

package org.springframework.cloud.task.configuration;

<<<<<<< 6319c712fa18a9e749ea9a97f1a12fe6fe568d25
import org.springframework.cloud.task.repository.support.LoggerTaskRepository;
=======
>>>>>>> Code Review Cleanup
import org.springframework.cloud.task.repository.LoggerTaskRepository;
import org.springframework.cloud.task.repository.TaskRepository;

/**
 * If no {@link TaskConfigurer} is present, then this configuration will be used.
 * The following defaults will be used:
 *
 * <ul>
 * <li>{@link LoggerTaskRepository} will be the default {@link TaskRepository}.</li>
 * </ul>
 *
 *
 * @author Glenn Renfro
 */
<<<<<<< 6319c712fa18a9e749ea9a97f1a12fe6fe568d25
public class DefaultTaskConfigurer implements TaskConfigurer{
=======
public class  DefaultTaskConfigurer implements TaskConfigurer{
>>>>>>> Code Review Cleanup

	public DefaultTaskConfigurer(){
	}

	public TaskRepository getTaskRepository() {
		return new LoggerTaskRepository();
	}

}
