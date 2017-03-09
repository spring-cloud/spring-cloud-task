/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 *  @author Ali Shahbour
 */
@ConfigurationProperties(prefix = "spring.cloud.task.batch.events")
public class TaskEventProperties {

    /**
     * Properties for jobExecution listener
     */
	private ListenerProperties jobExecution = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for stepExecution listener
     */
	private ListenerProperties stepExecution = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for itemRead listener
     */
	private ListenerProperties itemRead = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for itemProcess listener
     */
	private ListenerProperties itemProcess = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for itemWrite listener
     */
	private ListenerProperties itemWrite = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for chunk listener
     */
	private ListenerProperties chunk = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    /**
     * Properties for skip listener
     */
	private ListenerProperties skip = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

	public ListenerProperties getJobExecution() {
		return jobExecution;
	}

	public void setJobExecution(ListenerProperties jobExecution) {
		this.jobExecution = jobExecution;
	}

	public ListenerProperties getStepExecution() {
		return stepExecution;
	}

	public void setStepExecution(ListenerProperties stepExecution) {
		this.stepExecution = stepExecution;
	}

	public ListenerProperties getItemRead() {
		return itemRead;
	}

	public void setItemRead(ListenerProperties itemRead) {
		this.itemRead = itemRead;
	}

	public ListenerProperties getItemProcess() {
		return itemProcess;
	}

	public void setItemProcess(ListenerProperties itemProcess) {
		this.itemProcess = itemProcess;
	}

	public ListenerProperties getItemWrite() {
		return itemWrite;
	}

	public void setItemWrite(ListenerProperties itemWrite) {
		this.itemWrite = itemWrite;
	}

	public ListenerProperties getChunk() {
		return chunk;
	}

	public void setChunk(ListenerProperties chunk) {
		this.chunk = chunk;
	}

	public ListenerProperties getSkip() {
		return skip;
	}

	public void setSkip(ListenerProperties skip) {
		this.skip = skip;
	}

}
