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
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;

/**
 * Automates the creation of a {@link SimpleTaskExplorer} which will retrieve task
 * execution data from a in-memory map.
 *
 * @author Glenn Renfro
 */
public class MapTaskExplorerFactoryBean {

	private static final Log logger = LogFactory.getLog(MapTaskExplorerFactoryBean.class);

	public MapTaskExplorerFactoryBean(){

	}

	/**
	 * Returns the a simpleTaskExplorer that utilizes a MapTaskExecutionDao
	 * @return instance of task repository.
	 */
	public TaskExplorer getObject(){
		TaskExplorer taskExplorer = null;
		logger.debug(String.format("Creating SimpleTaskExplorer that will use a %s",
				MapTaskExecutionDao.class.getName()));
		taskExplorer =  new SimpleTaskExplorer(new MapTaskExecutionDao());
		return taskExplorer;
	}

}
