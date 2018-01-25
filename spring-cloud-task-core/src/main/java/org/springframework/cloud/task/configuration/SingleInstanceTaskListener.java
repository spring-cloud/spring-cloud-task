/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.task.listener.TaskExecutionException;
import org.springframework.cloud.task.listener.annotation.AfterTask;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.listener.annotation.FailedTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.leader.DefaultCandidate;
import org.springframework.integration.leader.event.OnFailedToAcquireMutexEvent;
import org.springframework.integration.leader.event.OnGrantedEvent;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * When spring.cloud.task.singleInstanceEnabled is set to true this listener will create a lock for the task
 * based on the spring.cloud.task.name. If a lock already exists this Listener will throw
 * a TaskExecutionException.  If this listener is added manually, then it should
 * be added as the first listener in the chain.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
public class SingleInstanceTaskListener implements ApplicationListener<ApplicationEvent> {

	private final static Log logger = LogFactory.getLog(SingleInstanceTaskListener.class);

	private LockRegistry lockRegistry;

	private LockRegistryLeaderInitiator lockRegistryLeaderInitiator;

	private TaskNameResolver taskNameResolver;

	private ApplicationEventPublisher applicationEventPublisher;

	private boolean lockReady;

	private boolean lockFailed;

	private DataSource dataSource;

	private TaskProperties taskProperties;

	public SingleInstanceTaskListener(LockRegistry lockRegistry,
			TaskNameResolver taskNameResolver,
			TaskProperties taskProperties,
			ApplicationEventPublisher applicationEventPublisher) {
		this.lockRegistry = lockRegistry;
		this.taskNameResolver = taskNameResolver;
		this.taskProperties = taskProperties;
		this.lockRegistryLeaderInitiator = new LockRegistryLeaderInitiator(this.lockRegistry);
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public SingleInstanceTaskListener(DataSource dataSource,
			TaskNameResolver taskNameResolver,
			TaskProperties taskProperties,
			ApplicationEventPublisher applicationEventPublisher) {
		this.taskNameResolver = taskNameResolver;
		this.applicationEventPublisher = applicationEventPublisher;
		this.dataSource = dataSource;
		this.taskProperties = taskProperties;
	}

	@BeforeTask
	public void lockTask(TaskExecution taskExecution) {
		if(this.lockRegistry == null ) {
			this.lockRegistry = getDefaultLockRegistry(taskExecution.getExecutionId());
		}
		this.lockRegistryLeaderInitiator = new LockRegistryLeaderInitiator(
				this.lockRegistry,
				new DefaultCandidate(String.valueOf(taskExecution.getExecutionId()),
						taskNameResolver.getTaskName()));
		this.lockRegistryLeaderInitiator.setApplicationEventPublisher(this.applicationEventPublisher);
		this.lockRegistryLeaderInitiator.setPublishFailedEvents(true);
		this.lockRegistryLeaderInitiator.start();
		while (!this.lockReady) {
			try {
				Thread.sleep(this.taskProperties.getSingleInstanceLockCheckInterval());
			}
			catch (InterruptedException ex) {
				logger.warn("Thread Sleep Failed", ex);
			}
			if (this.lockFailed) {
				String errorMessage = String.format(
						"Task with name \"%s\" is already running.",
						this.taskNameResolver.getTaskName());
				try {
					this.lockRegistryLeaderInitiator.destroy();
				}
				catch (Exception exception) {
					throw new TaskExecutionException("Failed to destroy lock.", exception);
				}
				throw new TaskExecutionException(errorMessage);
			}
		}
	}

	@AfterTask
	public void unlockTaskOnEnd(TaskExecution taskExecution) throws Exception {
		this.lockRegistryLeaderInitiator.destroy();
	}

	@FailedTask
	public void unlockTaskOnError(TaskExecution taskExecution) throws Exception {
		this.lockRegistryLeaderInitiator.destroy();
	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationEvent) {
		if (applicationEvent instanceof OnGrantedEvent) {
			this.lockReady = true;
		}
		else if (applicationEvent instanceof OnFailedToAcquireMutexEvent) {
			this.lockFailed = true;
		}
	}

	private LockRegistry getDefaultLockRegistry( long executionId) {
		DefaultLockRepository lockRepository =
				new DefaultLockRepository(this.dataSource, String.valueOf(
						executionId));
		lockRepository.setPrefix(this.taskProperties.getTablePrefix());
		lockRepository.setTimeToLive(this.taskProperties.getSingleInstanceLockTtl());
		lockRepository.afterPropertiesSet();
		return  new JdbcLockRegistry(lockRepository);
	}
}
