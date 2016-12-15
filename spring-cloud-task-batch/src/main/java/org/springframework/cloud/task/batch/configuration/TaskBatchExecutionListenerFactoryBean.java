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
package org.springframework.cloud.task.batch.configuration;

import java.lang.reflect.Field;
import javax.sql.DataSource;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.batch.listener.TaskBatchExecutionListener;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.MapTaskBatchDao;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link FactoryBean} for a {@link TaskBatchExecutionListener}.  Provides a jdbc based
 * listener if there is a {@link DataSource} available.  Otherwise, builds a listener that
 * uses the map based implementation.
 *
 * @author Michael Minella
 */
public class TaskBatchExecutionListenerFactoryBean implements FactoryBean<TaskBatchExecutionListener> {

	private TaskBatchExecutionListener listener;

	private DataSource dataSource;

	private TaskExplorer taskExplorer;

	private String tablePrefix = TaskProperties.DEFAULT_TABLE_PREFIX;

	/**
	 * Initializes the TaskBatchExecutionListenerFactoryBean and defaults the
	 * tablePrefix to {@link TaskProperties#DEFAULT_TABLE_PREFIX}.
	 *
	 * @param dataSource the dataSource to use for the TaskBatchExecutionListener.
	 * @param taskExplorer the taskExplorer to use for the TaskBatchExecutionListener.
	 */
	public TaskBatchExecutionListenerFactoryBean(DataSource dataSource, TaskExplorer taskExplorer) {
		this.dataSource = dataSource;
		this.taskExplorer = taskExplorer;
	}

	/**
	 * Initializes the TaskBatchExecutionListenerFactoryBean.
	 *
	 * @param dataSource the dataSource to use for the TaskBatchExecutionListener.
	 * @param taskExplorer the taskExplorer to use for the TaskBatchExecutionListener.
	 * @param tablePrefix the prefix for the task tables accessed by the
	 * TaskBatchExecutionListener.
	 */
	public TaskBatchExecutionListenerFactoryBean(DataSource dataSource, TaskExplorer taskExplorer, String tablePrefix) {
		this(dataSource,taskExplorer);
		Assert.hasText(tablePrefix, "tablePrefix must not be null nor empty.");
		this.tablePrefix = tablePrefix;
	}

	@Override
	public TaskBatchExecutionListener getObject() throws Exception {
		if(listener != null){
			return listener;
		}
		if(this.dataSource == null) {
			this.listener = new TaskBatchExecutionListener(getMapTaskBatchDao());
		}
		else {
			this.listener = new TaskBatchExecutionListener(
					new JdbcTaskBatchDao(this.dataSource, tablePrefix));
		}

		return listener;
	}

	@Override
	public Class<?> getObjectType() {
		return TaskBatchExecutionListener.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private MapTaskBatchDao getMapTaskBatchDao() throws Exception {
		Field taskExecutionDaoField = ReflectionUtils.findField(SimpleTaskExplorer.class, "taskExecutionDao");
		taskExecutionDaoField.setAccessible(true);

		MapTaskExecutionDao taskExecutionDao;

		if(AopUtils.isJdkDynamicProxy(this.taskExplorer)) {
			SimpleTaskExplorer dereferencedTaskRepository = (SimpleTaskExplorer) ((Advised) this.taskExplorer).getTargetSource().getTarget();

			taskExecutionDao =
					(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, dereferencedTaskRepository);
		}
		else {
			taskExecutionDao =
					(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, this.taskExplorer);
		}

		return new MapTaskBatchDao(taskExecutionDao.getBatchJobAssociations());
	}
}
