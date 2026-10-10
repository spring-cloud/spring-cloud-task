/*
 * Copyright 2015-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.repository.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Data Access Object for task executions using MongoDB.
 *
 * @author JongJun Kim
 */
public class MongoTaskExecutionDao implements TaskExecutionDao {

	private static final String TASK_EXECUTION_COLLECTION = "task_executions";
	private static final String TASK_EXECUTION_PARAMS_COLLECTION = "task_execution_parameters";
	private static final String TASK_BATCH_COLLECTION = "task_batch_associations";
	private static final String TASK_SEQUENCE_COLLECTION = "task_sequence";

	private static final String TASK_EXECUTION_ID_KEY = "taskExecutionId";
	private static final String TASK_NAME_KEY = "taskName";
	private static final String START_TIME_KEY = "startTime";
	private static final String END_TIME_KEY = "endTime";
	private static final String EXIT_CODE_KEY = "exitCode";
	private static final String EXIT_MESSAGE_KEY = "exitMessage";
	private static final String ERROR_MESSAGE_KEY = "errorMessage";
	private static final String LAST_UPDATED_KEY = "lastUpdated";
	private static final String EXTERNAL_EXECUTION_ID_KEY = "externalExecutionId";
	private static final String PARENT_EXECUTION_ID_KEY = "parentExecutionId";
	private static final String TASK_PARAM_KEY = "taskParam";
	private static final String JOB_EXECUTION_ID_KEY = "jobExecutionId";

	private final MongoOperations mongoOperations;

	private final String tablePrefix;

	public MongoTaskExecutionDao(MongoOperations mongoOperations, TaskProperties taskProperties) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null");
		Assert.notNull(taskProperties, "taskProperties must not be null");
		this.mongoOperations = mongoOperations;
		this.tablePrefix = taskProperties.getTablePrefix();
	}

	/**
	 * Constructor that accepts Object type to maintain backward compatibility with
	 * {@link org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean}.
	 * <p>
	 * This constructor exists because TaskExecutionDaoFactoryBean is in the core module
	 * and cannot have a compile-time dependency on MongoDB (which is optional).
	 * The factory uses reflection to instantiate this class only when MongoDB is available.
	 * <p>
	 * <b>Note:</b> Direct users should prefer the type-safe constructor
	 * {@link #MongoTaskExecutionDao(MongoOperations, TaskProperties)}.
	 *
	 * @param mongoOperations the MongoOperations instance (passed as Object to avoid compile-time dependency)
	 * @param taskProperties the task properties
	 * @throws IllegalArgumentException if mongoOperations is not a MongoOperations instance
	 */
	public MongoTaskExecutionDao(Object mongoOperations, TaskProperties taskProperties) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null");
		Assert.notNull(taskProperties, "taskProperties must not be null");

		if (!(mongoOperations instanceof MongoOperations)) {
			throw new IllegalArgumentException(
				String.format("Provided object is not a MongoOperations instance. Found: %s",
					mongoOperations.getClass().getName()));
		}

		this.mongoOperations = (MongoOperations) mongoOperations;
		this.tablePrefix = taskProperties.getTablePrefix();
	}

	private String getCollectionName(String collectionName) {
		return this.tablePrefix + collectionName;
	}

	@Override
	public TaskExecution createTaskExecution(String taskName, LocalDateTime startTime, List<String> arguments,
			String externalExecutionId) {
		return createTaskExecution(taskName, startTime, arguments, externalExecutionId, null);
	}

	@Override
	public TaskExecution createTaskExecution(String taskName, LocalDateTime startTime, List<String> arguments,
			String externalExecutionId, Long parentExecutionId) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Assert.notNull(startTime, "startTime must not be null");

		long taskExecutionId = getNextExecutionId();

		TaskExecutionDocument document = new TaskExecutionDocument();
		document.setTaskExecutionId(taskExecutionId);
		document.setTaskName(taskName);
		document.setStartTime(startTime);
		document.setExternalExecutionId(externalExecutionId);
		document.setParentExecutionId(parentExecutionId);
		document.setLastUpdated(LocalDateTime.now());

		mongoOperations.save(document, getCollectionName(TASK_EXECUTION_COLLECTION));

		// Save parameters
		if (arguments != null) {
			for (String argument : arguments) {
				if (argument != null) {
					TaskExecutionParameterDocument paramDoc = new TaskExecutionParameterDocument();
					paramDoc.setTaskExecutionId(taskExecutionId);
					paramDoc.setTaskParam(argument);
					mongoOperations.save(paramDoc, getCollectionName(TASK_EXECUTION_PARAMS_COLLECTION));
				}
			}
		}

		return convertToTaskExecution(document, arguments);
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName, LocalDateTime startTime,
			List<String> arguments, String externalExecutionId) {
		return startTaskExecution(executionId, taskName, startTime, arguments, externalExecutionId, null);
	}

	@Override
	public TaskExecution startTaskExecution(long executionId, String taskName, LocalDateTime startTime,
			List<String> arguments, String externalExecutionId, Long parentExecutionId) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Assert.notNull(startTime, "startTime must not be null");

		// Create new task execution document
		TaskExecutionDocument document = new TaskExecutionDocument();
		document.setTaskExecutionId(executionId);
		document.setTaskName(taskName);
		document.setStartTime(startTime);
		document.setExternalExecutionId(externalExecutionId);
		document.setParentExecutionId(parentExecutionId);
		document.setLastUpdated(LocalDateTime.now());

		mongoOperations.save(document, getCollectionName(TASK_EXECUTION_COLLECTION));

		// Save parameters
		if (arguments != null) {
			for (String argument : arguments) {
				if (argument != null) {
					TaskExecutionParameterDocument paramDoc = new TaskExecutionParameterDocument();
					paramDoc.setTaskExecutionId(executionId);
					paramDoc.setTaskParam(argument);
					mongoOperations.save(paramDoc, getCollectionName(TASK_EXECUTION_PARAMS_COLLECTION));
				}
			}
		}

		return convertToTaskExecution(document, arguments);
	}

	@Override
	public void completeTaskExecution(long executionId, Integer exitCode, LocalDateTime endTime, String exitMessage,
			String errorMessage) {
		Assert.notNull(endTime, "endTime must not be null");

		// Check if task execution exists first
		TaskExecution taskExecution = getTaskExecution(executionId);
		if (taskExecution == null) {
			throw new IllegalStateException("Invalid TaskExecution, ID " + executionId + " not found.");
		}

		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(executionId));
		Update update = new Update()
			.set(EXIT_CODE_KEY, exitCode)
			.set(END_TIME_KEY, endTime)
			.set(EXIT_MESSAGE_KEY, exitMessage)
			.set(ERROR_MESSAGE_KEY, errorMessage)
			.set(LAST_UPDATED_KEY, LocalDateTime.now());

		mongoOperations.updateFirst(query, update, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public void completeTaskExecution(long executionId, Integer exitCode, LocalDateTime endTime, String exitMessage) {
		completeTaskExecution(executionId, exitCode, endTime, exitMessage, null);
	}

	@Override
	public TaskExecution getTaskExecution(long executionId) {
		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(executionId));
		TaskExecutionDocument document = mongoOperations.findOne(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		if (document == null) {
			return null;
		}

		List<String> arguments = getTaskExecutionParameters(executionId);
		return convertToTaskExecution(document, arguments);
	}

	@Override
	public long getTaskExecutionCountByTaskName(String taskName) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Query query = new Query(Criteria.where(TASK_NAME_KEY).is(taskName));
		return mongoOperations.count(query, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public long getRunningTaskExecutionCountByTaskName(String taskName) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Query query = new Query(Criteria.where(TASK_NAME_KEY).is(taskName).and(END_TIME_KEY).isNull());
		return mongoOperations.count(query, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public long getRunningTaskExecutionCount() {
		Query query = new Query(Criteria.where(END_TIME_KEY).isNull());
		return mongoOperations.count(query, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public long getTaskExecutionCount() {
		return mongoOperations.count(new Query(), getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public Page<TaskExecution> findRunningTaskExecutions(String taskName, Pageable pageable) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Assert.notNull(pageable, "pageable must not be null");

		Query query = new Query(Criteria.where(TASK_NAME_KEY).is(taskName).and(END_TIME_KEY).isNull())
			.with(pageable);

		List<TaskExecutionDocument> documents = mongoOperations.find(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		long total = getRunningTaskExecutionCountByTaskName(taskName);
		List<TaskExecution> taskExecutions = convertToTaskExecutions(documents);

		return new PageImpl<>(taskExecutions, pageable, total);
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByExternalExecutionId(String externalExecutionId, Pageable pageable) {
		Assert.hasText(externalExecutionId, "externalExecutionId must not be null or empty");
		Assert.notNull(pageable, "pageable must not be null");

		Query query = new Query(Criteria.where(EXTERNAL_EXECUTION_ID_KEY).is(externalExecutionId))
			.with(pageable);

		List<TaskExecutionDocument> documents = mongoOperations.find(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		long total = getTaskExecutionCountByExternalExecutionId(externalExecutionId);
		List<TaskExecution> taskExecutions = convertToTaskExecutions(documents);

		return new PageImpl<>(taskExecutions, pageable, total);
	}

	@Override
	public long getTaskExecutionCountByExternalExecutionId(String externalExecutionId) {
		Assert.hasText(externalExecutionId, "externalExecutionId must not be null or empty");
		Query query = new Query(Criteria.where(EXTERNAL_EXECUTION_ID_KEY).is(externalExecutionId));
		return mongoOperations.count(query, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	@Override
	public Page<TaskExecution> findTaskExecutionsByName(String taskName, Pageable pageable) {
		Assert.hasText(taskName, "taskName must not be null or empty");
		Assert.notNull(pageable, "pageable must not be null");

		Query query = new Query(Criteria.where(TASK_NAME_KEY).is(taskName))
			.with(pageable);

		List<TaskExecutionDocument> documents = mongoOperations.find(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		long total = getTaskExecutionCountByTaskName(taskName);
		List<TaskExecution> taskExecutions = convertToTaskExecutions(documents);

		return new PageImpl<>(taskExecutions, pageable, total);
	}

	@Override
	public List<String> getTaskNames() {
		Query query = new Query();
		return mongoOperations.findDistinct(query, TASK_NAME_KEY, getCollectionName(TASK_EXECUTION_COLLECTION), String.class);
	}

	@Override
	public Page<TaskExecution> findAll(Pageable pageable) {
		Assert.notNull(pageable, "pageable must not be null");

		Query query = new Query().with(pageable);

		List<TaskExecutionDocument> documents = mongoOperations.find(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		long total = getTaskExecutionCount();
		List<TaskExecution> taskExecutions = convertToTaskExecutions(documents);

		return new PageImpl<>(taskExecutions, pageable, total);
	}

	@Override
	public long getNextExecutionId() {
		Query query = new Query(Criteria.where("_id").is("task_seq"));
		Update update = new Update().inc("sequence", 1);
		FindAndModifyOptions options = new FindAndModifyOptions().upsert(true).returnNew(true);
		TaskSequence sequence = mongoOperations.findAndModify(query, update, options, TaskSequence.class, getCollectionName(TASK_SEQUENCE_COLLECTION));
		return sequence != null ? sequence.getSequence() : 1L;
	}

	@Override
	public Long getTaskExecutionIdByJobExecutionId(long jobExecutionId) {
		Query query = new Query(Criteria.where(JOB_EXECUTION_ID_KEY).is(jobExecutionId));
		TaskBatchAssociationDocument association = mongoOperations.findOne(query,
			TaskBatchAssociationDocument.class, getCollectionName(TASK_BATCH_COLLECTION));

		return association != null ? association.getTaskExecutionId() : null;
	}

	@Override
	public Set<Long> getJobExecutionIdsByTaskExecutionId(long taskExecutionId) {
		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(taskExecutionId));
		List<TaskBatchAssociationDocument> associations = mongoOperations.find(query,
			TaskBatchAssociationDocument.class, getCollectionName(TASK_BATCH_COLLECTION));

		Set<Long> jobExecutionIds = new HashSet<>();
		for (TaskBatchAssociationDocument association : associations) {
			jobExecutionIds.add(association.getJobExecutionId());
		}

		return jobExecutionIds;
	}

	@Override
	public void updateExternalExecutionId(long taskExecutionId, String externalExecutionId) {
		// Check if task execution exists first
		TaskExecution taskExecution = getTaskExecution(taskExecutionId);
		Assert.notNull(taskExecution, "Invalid TaskExecution, ID " + taskExecutionId + " not found.");

		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(taskExecutionId));
		Update update = new Update()
			.set(EXTERNAL_EXECUTION_ID_KEY, externalExecutionId)
			.set(LAST_UPDATED_KEY, LocalDateTime.now());

		mongoOperations.updateFirst(query, update, getCollectionName(TASK_EXECUTION_COLLECTION));
	}

	/**
	 * Create a batch-task association.
	 * @param taskExecutionId The task execution ID
	 * @param jobExecutionId The batch job execution ID
	 */
	public void createTaskBatchAssociation(long taskExecutionId, long jobExecutionId) {
		TaskBatchAssociationDocument association = new TaskBatchAssociationDocument();
		association.setTaskExecutionId(taskExecutionId);
		association.setJobExecutionId(jobExecutionId);

		mongoOperations.save(association, getCollectionName(TASK_BATCH_COLLECTION));
	}

	/**
	 * Delete batch-task association by task execution ID.
	 * @param taskExecutionId The task execution ID
	 */
	public void deleteTaskBatchAssociationsByTaskExecutionId(long taskExecutionId) {
		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(taskExecutionId));
		mongoOperations.remove(query, TaskBatchAssociationDocument.class,
			getCollectionName(TASK_BATCH_COLLECTION));
	}

	/**
	 * Delete batch-task association by job execution ID.
	 * @param jobExecutionId The batch job execution ID
	 */
	public void deleteTaskBatchAssociationsByJobExecutionId(long jobExecutionId) {
		Query query = new Query(Criteria.where(JOB_EXECUTION_ID_KEY).is(jobExecutionId));
		mongoOperations.remove(query, TaskBatchAssociationDocument.class,
			getCollectionName(TASK_BATCH_COLLECTION));
	}

	@Override
	public List<TaskExecution> getLatestTaskExecutionsByTaskNames(String... taskNames) {
		Assert.notEmpty(taskNames, "At least 1 task name must be provided.");

		final List<String> taskNamesAsList = new ArrayList<>();

		for (String taskName : taskNames) {
			if (StringUtils.hasText(taskName)) {
				taskNamesAsList.add(taskName);
			}
		}

		Assert.isTrue(taskNamesAsList.size() == taskNames.length,
			String.format("Task names must not contain any empty elements but %s of %s were empty or null.",
				taskNames.length - taskNamesAsList.size(), taskNames.length));

		List<TaskExecution> result = new ArrayList<>();
		for (String taskName : taskNamesAsList) {
			TaskExecution latest = getLatestTaskExecutionForTaskName(taskName);
			if (latest != null) {
				result.add(latest);
			}
		}

		// Sort by startTime DESC, then by executionId DESC (latest first)
		result.sort((a, b) -> {
			int timeCompare = b.getStartTime().compareTo(a.getStartTime());
			if (timeCompare != 0) {
				return timeCompare;
			}
			return Long.compare(b.getExecutionId(), a.getExecutionId());
		});

		return result;
	}

	@Override
	public TaskExecution getLatestTaskExecutionForTaskName(String taskName) {
		Assert.hasText(taskName, "The task name must not be empty.");

		Query query = new Query(Criteria.where(TASK_NAME_KEY).is(taskName))
			.with(Sort.by(Sort.Direction.DESC, START_TIME_KEY, TASK_EXECUTION_ID_KEY))
			.limit(1);

		TaskExecutionDocument document = mongoOperations.findOne(query, TaskExecutionDocument.class,
			getCollectionName(TASK_EXECUTION_COLLECTION));

		if (document == null) {
			return null;
		}

		List<String> arguments = getTaskExecutionParameters(document.getTaskExecutionId());
		return convertToTaskExecution(document, arguments);
	}

	private List<String> getTaskExecutionParameters(long taskExecutionId) {
		Query query = new Query(Criteria.where(TASK_EXECUTION_ID_KEY).is(taskExecutionId));
		List<TaskExecutionParameterDocument> paramDocs = mongoOperations.find(query,
			TaskExecutionParameterDocument.class, getCollectionName(TASK_EXECUTION_PARAMS_COLLECTION));

		List<String> arguments = new ArrayList<>();
		for (TaskExecutionParameterDocument paramDoc : paramDocs) {
			arguments.add(paramDoc.getTaskParam());
		}

		return arguments;
	}

	private List<TaskExecution> convertToTaskExecutions(List<TaskExecutionDocument> documents) {
		List<TaskExecution> taskExecutions = new ArrayList<>();

		for (TaskExecutionDocument document : documents) {
			List<String> arguments = getTaskExecutionParameters(document.getTaskExecutionId());
			taskExecutions.add(convertToTaskExecution(document, arguments));
		}

		return taskExecutions;
	}

	private TaskExecution convertToTaskExecution(TaskExecutionDocument document, List<String> arguments) {
		if (document == null) {
			return null;
		}

		return new TaskExecution(
			document.getTaskExecutionId(),
			document.getExitCode(),
			document.getTaskName(),
			document.getStartTime(),
			document.getEndTime(),
			document.getExitMessage(),
			arguments != null ? arguments : Collections.emptyList(),
			document.getErrorMessage(),
			document.getExternalExecutionId(),
			document.getParentExecutionId()
		);
	}

	// MongoDB Document Classes
	// Note: These classes use simple POJOs without @Document annotations because
	// collections are specified dynamically at runtime with table prefixes.
	// The mongoOperations.save(object, collectionName) pattern is used instead.
	public static class TaskExecutionDocument {
		private Long taskExecutionId;
		private LocalDateTime startTime;
		private LocalDateTime endTime;
		private String taskName;
		private Integer exitCode;
		private String exitMessage;
		private String errorMessage;
		private LocalDateTime lastUpdated;
		private String externalExecutionId;
		private Long parentExecutionId;

		// Getters and setters
		public Long getTaskExecutionId() {
			return taskExecutionId;
		}
		public void setTaskExecutionId(Long taskExecutionId) {
			this.taskExecutionId = taskExecutionId;
		}
		public LocalDateTime getStartTime() {
			return startTime;
		}
		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}
		public LocalDateTime getEndTime() {
			return endTime;
		}
		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}
		public String getTaskName() {
			return taskName;
		}
		public void setTaskName(String taskName) {
			this.taskName = taskName;
		}
		public Integer getExitCode() {
			return exitCode;
		}
		public void setExitCode(Integer exitCode) {
			this.exitCode = exitCode;
		}
		public String getExitMessage() {
			return exitMessage;
		}
		public void setExitMessage(String exitMessage) {
			this.exitMessage = exitMessage;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		public LocalDateTime getLastUpdated() {
			return lastUpdated;
		}
		public void setLastUpdated(LocalDateTime lastUpdated) {
			this.lastUpdated = lastUpdated;
		}
		public String getExternalExecutionId() {
			return externalExecutionId;
		}
		public void setExternalExecutionId(String externalExecutionId) {
			this.externalExecutionId = externalExecutionId;
		}
		public Long getParentExecutionId() {
			return parentExecutionId;
		}
		public void setParentExecutionId(Long parentExecutionId) {
			this.parentExecutionId = parentExecutionId;
		}
	}

	public static class TaskExecutionParameterDocument {
		private Long taskExecutionId;
		private String taskParam;

		public Long getTaskExecutionId() {
			return taskExecutionId;
		}
		public void setTaskExecutionId(Long taskExecutionId) {
			this.taskExecutionId = taskExecutionId;
		}
		public String getTaskParam() {
			return taskParam;
		}
		public void setTaskParam(String taskParam) {
			this.taskParam = taskParam;
		}
	}

	public static class TaskBatchAssociationDocument {
		private Long taskExecutionId;
		private Long jobExecutionId;

		public Long getTaskExecutionId() {
			return taskExecutionId;
		}
		public void setTaskExecutionId(Long taskExecutionId) {
			this.taskExecutionId = taskExecutionId;
		}
		public Long getJobExecutionId() {
			return jobExecutionId;
		}
		public void setJobExecutionId(Long jobExecutionId) {
			this.jobExecutionId = jobExecutionId;
		}
	}

	public static class TaskSequence {
		private String id;
		private Long sequence;

		public TaskSequence() {
		}

		public TaskSequence(String id, Long sequence) {
			this.id = id;
			this.sequence = sequence;
		}

		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Long getSequence() {
			return sequence;
		}
		public void setSequence(Long sequence) {
			this.sequence = sequence;
		}
	}
}
