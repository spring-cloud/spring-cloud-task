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
     * Properties for jobExecution listener order
     */
	private int jobExecutionOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for stepExecution listener order
     */
	private int stepExecutionOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for itemRead listener order
     */
	private int itemReadOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for itemProcess listener order
     */
	private int itemProcessOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for itemWrite listener order
     */
	private int itemWriteOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for chunk listener order
     */
	private int chunkOrder = Ordered.LOWEST_PRECEDENCE;

    /**
     * Properties for skip listener order
     */
	private int skipOrder = Ordered.LOWEST_PRECEDENCE;

	public int getJobExecutionOrder() {
		return jobExecutionOrder;
	}

	public void setJobExecutionOrder(int jobExecutionOrder) {
		this.jobExecutionOrder = jobExecutionOrder;
	}

	public int getStepExecutionOrder() {
		return stepExecutionOrder;
	}

	public void setStepExecutionOrder(int stepExecutionOrder) {
		this.stepExecutionOrder = stepExecutionOrder;
	}

	public int getItemReadOrder() {
		return itemReadOrder;
	}

	public void setItemReadOrder(int itemReadOrder) {
		this.itemReadOrder = itemReadOrder;
	}

	public int getItemProcessOrder() {
		return itemProcessOrder;
	}

	public void setItemProcessOrder(int itemProcessOrder) {
		this.itemProcessOrder = itemProcessOrder;
	}

	public int getItemWriteOrder() {
		return itemWriteOrder;
	}

	public void setItemWriteOrder(int itemWriteOrder) {
		this.itemWriteOrder = itemWriteOrder;
	}

	public int getChunkOrder() {
		return chunkOrder;
	}

	public void setChunkOrder(int chunkOrder) {
		this.chunkOrder = chunkOrder;
	}

	public int getSkipOrder() {
		return skipOrder;
	}

	public void setSkipOrder(int skipOrder) {
		this.skipOrder = skipOrder;
	}
}
