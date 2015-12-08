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

package org.springframework.cloud.task.repository.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;

/**
 * Automates the creation of a {@link SimpleTaskRepository} which will persist task
 * execution data into an in memory map. This is meant for development
 * purposes and not for production use.
 *
 * @author Glenn Renfro
 */
public class MapTaskRepositoryFactoryBean {

	private static final Log logger = LogFactory.getLog(MapTaskRepositoryFactoryBean.class);

	private TaskRepository taskRepository;

	public MapTaskRepositoryFactoryBean(){
		logger.debug("Creating SimpleTaskRepository that will use a MapTaskExecutionDao");
		taskRepository = new SimpleTaskRepository(new MapTaskExecutionDao());
	}

	/**
	 * Returns the a simpleTaskRepository that utilizes a MapTaskExecutionDao
	 * @return instance of task repository.
	 */
	public TaskRepository getObject(){
		return taskRepository;
	}
}
