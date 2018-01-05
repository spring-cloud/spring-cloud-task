/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * This is a JobEvent DTO created so that a {@link org.springframework.batch.core.JobExecution} can be serialized into
 * Json without having to add mixins to an ObjectMapper.
 * @author Glenn Renfro
 */
public class JobExecutionEvent extends Entity {

	private JobParametersEvent jobParameters;

	private JobInstanceEvent jobInstance;

	private Collection<StepExecutionEvent> stepExecutions = new CopyOnWriteArraySet<StepExecutionEvent>();

	private BatchStatus status = BatchStatus.STARTING;

	private Date startTime = null;

	private Date createTime = new Date(System.currentTimeMillis());

	private Date endTime = null;

	private Date lastUpdated = null;

	private ExitStatus exitStatus = new ExitStatus(new org.springframework.batch.core.ExitStatus("UNKNOWN", null));

	private ExecutionContext executionContext = new ExecutionContext();

	private List<Throwable> failureExceptions = new CopyOnWriteArrayList<Throwable>();

	private String jobConfigurationName;

	public JobExecutionEvent() {
		super();
	}

	/**
	 * Constructor for the StepExecution to initialize the DTO.
	 *
	 * @param original the StepExecution to build this DTO around.
	 */
	public JobExecutionEvent(JobExecution original) {
		this.jobParameters = new JobParametersEvent(original.getJobParameters().getParameters());
		this.jobInstance = new JobInstanceEvent(original.getJobInstance().getId(), original.getJobInstance().getJobName());
		for(StepExecution stepExecution : original.getStepExecutions()){
			stepExecutions.add(new StepExecutionEvent(stepExecution));
		}
		this.status = original.getStatus();
		this.startTime = original.getStartTime();
		this.createTime = original.getCreateTime();
		this.endTime = original.getEndTime();
		this.lastUpdated = original.getLastUpdated();
		this.exitStatus = new ExitStatus(original.getExitStatus());
		this.executionContext = original.getExecutionContext();
		this.failureExceptions = original.getFailureExceptions();
		this.jobConfigurationName = original.getJobConfigurationName();
		this.setId(original.getId());
		this.setVersion(original.getVersion());
	}

	public JobParametersEvent getJobParameters() {
		return this.jobParameters;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	public void setJobInstance(JobInstanceEvent jobInstance) {
		this.jobInstance = jobInstance;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public BatchStatus getStatus() {
		return this.status;
	}

	/**
	 * Set the value of the status field.
	 *
	 * @param status the status to set
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * Upgrade the status field if the provided value is greater than the
	 * existing one. Clients using this method to set the status can be sure
	 * that they don't overwrite a failed status with an successful one.
	 *
	 * @param status the new status value
	 */
	public void upgradeStatus(BatchStatus status) {
		this.status = this.status.upgradeTo(status);
	}

	/**
	 * Convenience getter for for the id of the enclosing job. Useful for DAO
	 * implementations.
	 *
	 * @return the id of the enclosing job
	 */
	public Long getJobId() {
		if (jobInstance != null) {
			return jobInstance.getId();
		}
		return null;
	}

	/**
	 * @param exitStatus
	 */
	public void setExitStatus(ExitStatus exitStatus) {
		this.exitStatus = exitStatus;
	}

	/**
	 * @return the exitCode
	 */
	public ExitStatus getExitStatus() {
		return this.exitStatus;
	}

	/**
	 * @return the Job that is executing.
	 */
	public JobInstanceEvent getJobInstance() {
		return this.jobInstance;
	}

	/**
	 * Accessor for the step executions.
	 *
	 * @return the step executions that were registered
	 */
	public Collection<StepExecutionEvent> getStepExecutions() {
		return Collections.unmodifiableList(new ArrayList<>(this.stepExecutions));
	}

	/**
	 * Sets the {@link ExecutionContext} for this execution
	 *
	 * @param executionContext the context
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * Returns the {@link ExecutionContext} for this execution. The content is
	 * expected to be persisted after each step completion (successful or not).
	 *
	 * @return the context
	 */
	public ExecutionContext getExecutionContext() {
		return this.executionContext;
	}

	/**
	 * @return the time when this execution was created.
	 */
	public Date getCreateTime() {
		return this.createTime;
	}

	/**
	 * @param createTime creation time of this execution.
	 */
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getJobConfigurationName() {
		return this.jobConfigurationName;
	}

	/**
	 * Get the date representing the last time this JobExecution was updated in
	 * the JobRepository.
	 *
	 * @return Date representing the last time this JobExecution was updated.
	 */
	public Date getLastUpdated() {
		return this.lastUpdated;
	}

	/**
	 * Set the last time this JobExecution was updated.
	 *
	 * @param lastUpdated
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public List<Throwable> getFailureExceptions() {
		return this.failureExceptions;
	}

	/**
	 * Add the provided throwable to the failure exception list.
	 *
	 * @param t
	 */
	public synchronized void addFailureException(Throwable t) {
		this.failureExceptions.add(t);
	}

	/**
	 * Return all failure causing exceptions for this JobExecution, including
	 * step executions.
	 *
	 * @return List&lt;Throwable&gt; containing all exceptions causing failure for
	 * this JobExecution.
	 */
	public synchronized List<Throwable> getAllFailureExceptions() {

		Set<Throwable> allExceptions = new HashSet<>(this.failureExceptions);
		for (StepExecutionEvent stepExecution : this.stepExecutions) {
			allExceptions.addAll(stepExecution.getFailureExceptions());
		}

		return new ArrayList<>(allExceptions);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.Entity#toString()
	 */
	@Override
	public String toString() {
		return super.toString()
				+ String.format(", startTime=%s, endTime=%s, lastUpdated=%s, status=%s, exitStatus=%s, job=[%s], jobParameters=[%s]",
				startTime, endTime, lastUpdated, status, exitStatus, jobInstance, jobParameters);
	}
}
