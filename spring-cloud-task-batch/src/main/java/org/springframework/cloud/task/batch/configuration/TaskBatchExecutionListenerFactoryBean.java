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
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.MapTaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link FactoryBean} for a {@link TaskBatchExecutionListener}.  Provides a jdbc based
 * listener if there is a {@link DataSource} available.  Otherwise, builds a listener that
 * uses the map based implementation.
 *
 * @author Michael Minella
 */
public class TaskBatchExecutionListenerFactoryBean implements FactoryBean<TaskBatchExecutionListener> {

	private ConfigurableApplicationContext context;

	private TaskBatchExecutionListener listener;

	private String dataSourceName;

	/**
	 * @param context the current application context
	 */
	public TaskBatchExecutionListenerFactoryBean(ConfigurableApplicationContext context) {
		Assert.notNull(context, "A ConfigurableApplicationContext is required");

		this.context = context;
	}

	@Override
	public TaskBatchExecutionListener getObject() throws Exception {
		if(this.context.getBeanNamesForType(DataSource.class).length == 0) {
			this.listener = new TaskBatchExecutionListener(getMapTaskBatchDao());
		}
		else {
			DataSource dataSource;

			if(StringUtils.hasText(this.dataSourceName)) {
				dataSource = (DataSource) this.context.getBean(this.dataSourceName);
			}
			else {
				if(this.context.getBeanNamesForType(DataSource.class).length == 1) {
					dataSource = this.context.getBean(DataSource.class);
				}
				else {
					throw new IllegalStateException("Unable to determine what DataSource to use");
				}
			}
			this.listener = new TaskBatchExecutionListener(new JdbcTaskBatchDao(dataSource));
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

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	private MapTaskBatchDao getMapTaskBatchDao() throws Exception {
		Field taskExecutionDaoField = ReflectionUtils.findField(SimpleTaskExplorer.class, "taskExecutionDao");
		taskExecutionDaoField.setAccessible(true);

		MapTaskExecutionDao taskExecutionDao;

		TaskExplorer taskExplorer = this.context.getBean(TaskExplorer.class);

		if(AopUtils.isJdkDynamicProxy(taskExplorer)) {
			SimpleTaskExplorer dereferencedTaskRepository = (SimpleTaskExplorer) ((Advised) taskExplorer).getTargetSource().getTarget();

			taskExecutionDao =
					(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, dereferencedTaskRepository);
		}
		else {
			taskExecutionDao =
					(MapTaskExecutionDao) ReflectionUtils.getField(taskExecutionDaoField, taskExplorer);
		}

		return new MapTaskBatchDao(taskExecutionDao.getBatchJobAssociations());
	}
}
