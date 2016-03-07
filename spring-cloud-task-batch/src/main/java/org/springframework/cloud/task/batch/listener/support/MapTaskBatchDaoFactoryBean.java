/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.task.batch.listener.support;

import java.lang.reflect.Field;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael Minella
 */
public class MapTaskBatchDaoFactoryBean implements FactoryBean<MapTaskBatchDao> {

	private MapTaskBatchDao dao;

	private SimpleTaskExplorer taskExplorer;

	@Override
	public MapTaskBatchDao getObject() throws Exception {
		if(this.dao == null) {
			Field taskExecutionDaoField = ReflectionUtils.findField(SimpleTaskExplorer.class, "taskExecutionDao");
			taskExecutionDaoField.setAccessible(true);

			MapTaskExecutionDao taskExecutionDao;

			if(AopUtils.isJdkDynamicProxy(taskExplorer)) {
				SimpleTaskExplorer dereferencedTaskRepository = (SimpleTaskExplorer) ((Advised) taskExplorer).getTargetSource().getTarget();

				taskExecutionDao =
						(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, dereferencedTaskRepository);
			}
			else {
				taskExecutionDao =
						(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, taskExplorer);
			}

			this.dao = new MapTaskBatchDao(taskExecutionDao.getBatchJobAssociations());
		}

		return this.dao;
	}

	@Override
	public Class<?> getObjectType() {
		return MapTaskBatchDao.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	public void setTaskExplorer(SimpleTaskExplorer taskExplorer) {
		this.taskExplorer = taskExplorer;
	}
}
