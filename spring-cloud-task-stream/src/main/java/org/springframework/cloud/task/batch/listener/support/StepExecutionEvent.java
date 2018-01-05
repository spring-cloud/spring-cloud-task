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

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;

/**
 * This is a StepExecution DTO created so that a {@link org.springframework.batch.core.StepExecution} can be serialized
 * into Json without having to add mixins to an ObjectMapper.
 * @author Glenn Renfro
 */
public class StepExecutionEvent extends Entity {

	private long jobExecutionId;

	private String stepName;

	private BatchStatus status = BatchStatus.STARTING;

	private int readCount = 0;

	private int writeCount = 0;

	private int commitCount = 0;

	private int rollbackCount = 0;

	private int readSkipCount = 0;

	private int processSkipCount = 0;

	private int writeSkipCount = 0;

	private Date startTime = new Date(System.currentTimeMillis());

	private Date endTime = null;

	private Date lastUpdated = null;

	private ExecutionContext executionContext = new ExecutionContext();

	private ExitStatus exitStatus = new ExitStatus(org.springframework.batch.core.ExitStatus.EXECUTING);

	private boolean terminateOnly;

	private int filterCount;

	private List<Throwable> failureExceptions = new CopyOnWriteArrayList<Throwable>();


	public StepExecutionEvent() {
		super();
	}

	/**
	 * Constructor for the StepExecution to initialize the DTO.
	 *
	 * @param stepExecution the StepExecution to build this DTO around.
	 */
	public StepExecutionEvent(StepExecution stepExecution) {
		super();
		Assert.notNull(stepExecution, "StepExecution must be provided to re-hydrate an existing StepExecutionEvent");
		Assert.notNull(stepExecution.getJobExecution(), "JobExecution must be provided to re-hydrate an existing StepExecutionEvent");
		setId(stepExecution.getId());
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.stepName = stepExecution.getStepName();

		this.status = stepExecution.getStatus();
		this.exitStatus = new ExitStatus(stepExecution.getExitStatus());
		this.executionContext = stepExecution.getExecutionContext();
		for (Throwable throwable : stepExecution.getFailureExceptions()){
			this.failureExceptions.add(throwable);
		}
		this.terminateOnly = stepExecution.isTerminateOnly();

		this.endTime = stepExecution.getEndTime();
		this.lastUpdated = stepExecution.getLastUpdated();
		this.startTime = stepExecution.getStartTime();

		this.commitCount = stepExecution.getCommitCount();
		this.filterCount = stepExecution.getFilterCount();
		this.processSkipCount = stepExecution.getProcessSkipCount();
		this.readCount = stepExecution.getReadCount();
		this.readSkipCount = stepExecution.getReadSkipCount();
		this.rollbackCount = stepExecution.getRollbackCount();
		this.writeCount = stepExecution.getWriteCount();
		this.writeSkipCount = stepExecution.getWriteSkipCount();

	}

	/**
	 * Returns the {@link ExecutionContext} for this execution
	 *
	 * @return the attributes
	 */
	public ExecutionContext getExecutionContext() {
		return this.executionContext;
	}

	/**
	 * Sets the {@link ExecutionContext} for this execution
	 *
	 * @param executionContext the attributes
	 */
	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	/**
	 * Returns the current number of commits for this execution
	 *
	 * @return the current number of commits
	 */
	public int getCommitCount() {
		return this.commitCount;
	}

	/**
	 * Sets the current number of commits for this execution
	 *
	 * @param commitCount the current number of commits
	 */
	public void setCommitCount(int commitCount) {
		this.commitCount = commitCount;
	}

	/**
	 * Returns the time that this execution ended
	 *
	 * @return the time that this execution ended
	 */
	public Date getEndTime() {
		return this.endTime;
	}

	/**
	 * Sets the time that this execution ended
	 *
	 * @param endTime the time that this execution ended
	 */
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	/**
	 * Returns the current number of items read for this execution
	 *
	 * @return the current number of items read for this execution
	 */
	public int getReadCount() {
		return this.readCount;
	}

	/**
	 * Sets the current number of read items for this execution
	 *
	 * @param readCount the current number of read items for this execution
	 */
	public void setReadCount(int readCount) {
		this.readCount = readCount;
	}

	/**
	 * Returns the current number of items written for this execution
	 *
	 * @return the current number of items written for this execution
	 */
	public int getWriteCount() {
		return this.writeCount;
	}

	/**
	 * Sets the current number of written items for this execution
	 *
	 * @param writeCount the current number of written items for this execution
	 */
	public void setWriteCount(int writeCount) {
		this.writeCount = writeCount;
	}

	/**
	 * Returns the current number of rollbacks for this execution
	 *
	 * @return the current number of rollbacks for this execution
	 */
	public int getRollbackCount() {
		return this.rollbackCount;
	}

	/**
	 * Returns the current number of items filtered out of this execution
	 *
	 * @return the current number of items filtered out of this execution
	 */
	public int getFilterCount() {
		return this.filterCount;
	}

	/**
	 * Public setter for the number of items filtered out of this execution.
	 * @param filterCount the number of items filtered out of this execution to
	 * set
	 */
	public void setFilterCount(int filterCount) {
		this.filterCount = filterCount;
	}

	/**
	 * Setter for number of rollbacks for this execution
	 */
	public void setRollbackCount(int rollbackCount) {
		this.rollbackCount = rollbackCount;
	}

	/**
	 * Gets the time this execution started
	 *
	 * @return the time this execution started
	 */
	public Date getStartTime() {
		return this.startTime;
	}

	/**
	 * Sets the time this execution started
	 *
	 * @param startTime the time this execution started
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * Returns the current status of this step
	 *
	 * @return the current status of this step
	 */
	public BatchStatus getStatus() {
		return this.status;
	}

	/**
	 * Sets the current status of this step
	 *
	 * @param status the current status of this step
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}
	/**
	 * @return the name of the step
	 */
	public String getStepName() {
		return this.stepName;
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
	 * @return flag to indicate that an execution should halt
	 */
	public boolean isTerminateOnly() {
		return this.terminateOnly;
	}

	/**
	 * Set a flag that will signal to an execution environment that this
	 * execution (and its surrounding job) wishes to exit.
	 */
	public void setTerminateOnly() {
		this.terminateOnly = true;
	}

	/**
	 * @return the total number of items skipped.
	 */
	public int getSkipCount() {
		return this.readSkipCount + this.processSkipCount + this.writeSkipCount;
	}

	/**
	 * Increment the number of commits
	 */
	public void incrementCommitCount() {
		this.commitCount++;
	}

	/**
	 * @return the number of records skipped on read
	 */
	public int getReadSkipCount() {
		return this.readSkipCount;
	}

	/**
	 * @return the number of records skipped on write
	 */
	public int getWriteSkipCount() {
		return this.writeSkipCount;
	}

	/**
	 * Set the number of records skipped on read
	 *
	 * @param readSkipCount
	 */
	public void setReadSkipCount(int readSkipCount) {
		this.readSkipCount = readSkipCount;
	}

	/**
	 * Set the number of records skipped on write
	 *
	 * @param writeSkipCount
	 */
	public void setWriteSkipCount(int writeSkipCount) {
		this.writeSkipCount = writeSkipCount;
	}

	/**
	 * @return the number of records skipped during processing
	 */
	public int getProcessSkipCount() {
		return this.processSkipCount;
	}

	/**
	 * Set the number of records skipped during processing.
	 *
	 * @param processSkipCount
	 */
	public void setProcessSkipCount(int processSkipCount) {
		this.processSkipCount = processSkipCount;
	}

	/**
	 * @return the Date representing the last time this execution was persisted.
	 */
	public Date getLastUpdated() {
		return this.lastUpdated;
	}

	/**
	 * Set the time when the StepExecution was last updated before persisting
	 *
	 * @param lastUpdated
	 */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public List<Throwable> getFailureExceptions() {
		return this.failureExceptions;
	}

	public long getJobExecutionId() {
		return this.jobExecutionId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.container.common.domain.Entity#equals(java.
	 * lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if ( !(obj instanceof StepExecution) || getId() == null) {
			return super.equals(obj);
		}
		StepExecution other = (StepExecution) obj;

		return this.stepName.equals(other.getStepName()) && (this.jobExecutionId == other.getJobExecutionId())
				&& getId().equals(other.getId());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.container.common.domain.Entity#hashCode()
	 */
	@Override
	public int hashCode() {
		Object jobExecutionId = getJobExecutionId();
		Long id = getId();
		return super.hashCode() + 31 * (this.stepName != null ? this.stepName.hashCode() : 0) + 91
				* (jobExecutionId != null ? jobExecutionId.hashCode() : 0) + 59 * (id != null ? id.hashCode() : 0);
	}

	@Override
	public String toString() {
		return String.format(getSummary() + ", exitDescription=%s", this.exitStatus.getExitDescription());
	}

	public String getSummary() {
		return super.toString()
				+ String.format(
				", name=%s, status=%s, exitStatus=%s, readCount=%d, filterCount=%d, writeCount=%d readSkipCount=%d, writeSkipCount=%d"
						+ ", processSkipCount=%d, commitCount=%d, rollbackCount=%d", this.stepName, this.status,
				this.exitStatus.getExitCode(), this.readCount, this.filterCount, this.writeCount, this.readSkipCount, this.writeSkipCount,
				this.processSkipCount, this.commitCount, this.rollbackCount);
	}
}
