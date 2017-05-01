/*
 *  Copyright 2017 the original author or authors.
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

package io.spring.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity for the id and output to be written to the data store.
 * 
 * @author Pas Apicella
 * @author Glenn Renfro
 */
@Entity
@Table(name = "TASK_RUN_OUTPUT")
public class TaskRunOutput {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String output;

	public TaskRunOutput() {
	}

	public TaskRunOutput(String output) {
		this.output = output;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	@Override
	public String toString() {
		return "TaskRunOutput{" + "id=" + id + ", output='" + output + '\'' + '}';
	}
}
