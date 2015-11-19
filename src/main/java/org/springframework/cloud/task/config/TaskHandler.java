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
package org.springframework.cloud.task.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.cloud.task.annotation.Task;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Offers the advice on how to record tasks to the repository for both applicationrunner
 * and commandlinerunner spring boot applications.
 *
 * @author Glenn Renfro
 */
@Aspect
@Component
public class TaskHandler {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private TaskRepository repository;

	private String taskName;

	private String executionId;

	private TaskExecution taskExecution;

	/**
	 * Looks for any CommandLineRunner.run method with its class annotated with @Task
	 * and calls the repository implementation to store the start of the task in the repo
	 * before the run starts.
	 *
	 * @param joinPoint
	 */
	@Before("within( @org.springframework.cloud.task.annotation.Task *) && (execution(* org.springframework.boot.CommandLineRunner.run(..)) || execution(* org.springframework.boot.ApplicationRunner.run(..)))")
	public void beforeCommandLineRunner(JoinPoint joinPoint) {
		executionId = UUID.randomUUID().toString();
		taskExecution = new TaskExecution();

		Task a = joinPoint.getTarget().getClass().getAnnotation(Task.class);
		taskName = a.taskName();
		if (taskName == null || taskName.length() == 0) {
			taskName = joinPoint.getTarget().getClass().getName();
		}

		taskExecution.setTaskName(taskName);
		taskExecution.setStartTime(new Date());
		taskExecution.setExecutionId(executionId);
		repository.createTaskExecution(taskExecution);
	}

	/**
	 * Looks for any CommandLineRunner.run method with its class annotated with @Task
	 * and calls repository implementation to store the exit of the task in the repo after
	 * run returns result.
	 *
	 * @param joinPoint
	 */
	@AfterReturning("within( @org.springframework.cloud.task.annotation.Task *) && (execution(* org.springframework.boot.CommandLineRunner.run(..)) || execution(* org.springframework.boot.ApplicationRunner.run(..)))")
	public void afterReturnCommandLineRunner(JoinPoint joinPoint) {
		int result = 0;
		List<ExitCodeGenerator> generators = new ArrayList<ExitCodeGenerator>();
		generators
				.addAll(context.getBeansOfType(ExitCodeGenerator.class).values());
		for (ExitCodeGenerator generator : generators) {
			result = generator.getExitCode();
		}
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(result);
		repository.update(taskExecution);
	}

	/**
	 * Looks for any CommandLineRunner. run method with its class annotated with @Task
	 * and calls the repository implementation to store the exitCode of 1
	 * for the task in the repo in the case of an exception.
	 *
	 * @param joinPoint
	 */
	@AfterThrowing("within( @org.springframework.cloud.task.annotation.Task *) && (execution(* org.springframework.boot.CommandLineRunner.run(..)) || execution(* org.springframework.boot.ApplicationRunner.run(..)))")
	public void logExceptionCommandLineRunner(JoinPoint joinPoint) {
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(1);
		repository.update(taskExecution);
	}

}
