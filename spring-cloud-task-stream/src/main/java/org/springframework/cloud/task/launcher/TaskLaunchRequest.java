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

package org.springframework.cloud.task.launcher;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Request that contains the maven repository and property information required by the
 * TaskLauncherSink to launch the task.
 *
 * @author Glenn Renfro
 */
public class TaskLaunchRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	private String uri;
	private List<String> commandlineArguments;
	private Map<String, String> properties;

	/**
	 * Constructor for the TaskLaunchRequest;
	 * @param uri the URI to the task artifact to be launched.
	 * @param commandlineArguments list of commandlineArguments to be used by the task
	 * @param properties is the environment variables for this task.
	 */
	public TaskLaunchRequest(String uri, List<String> commandlineArguments,
							 Map<String, String> properties) {
		Assert.hasText(uri, "uri must not be empty nor null.");

		this.uri = uri;
		this.commandlineArguments = (commandlineArguments == null) ? new ArrayList<String>() : commandlineArguments;
		this.properties = properties == null ? new HashMap() : properties;
	}

	/**
	 * Returns the current uri to the artifact for this launch request.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Returns an unmodifiable list of arguments that will be used for the task execution
	 */
	public List<String> getCommandlineArguments() {
		return  Collections.unmodifiableList(commandlineArguments);
	}

	/**
	 * Retrieves the environment variables for the task.
	 * @return map containing the environment variables for the task.
	 */

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "TaskLaunchRequest{" +
				"uri='" + uri + '\'' +
				", commandlineArguments=" + commandlineArguments +
				", properties=" + properties +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o){
			return true;
		}
		if (o == null || getClass() != o.getClass()){
			return false;
		}

		TaskLaunchRequest that = (TaskLaunchRequest) o;

		if (!uri.equals(that.uri)){
			return false;
		}
		if (!(commandlineArguments != null ? commandlineArguments.equals(that.commandlineArguments) : that.commandlineArguments == null)){
			return false;
		}
		return properties != null ? properties.equals(that.properties) : that.properties == null;

	}

	@Override
	public int hashCode() {
		int result = uri != null ? uri.hashCode() : 0;
		result = 31 * result + (commandlineArguments != null ? commandlineArguments.hashCode() : 0);
		result = 31 * result + (properties != null ? properties.hashCode() : 0);
		return result;
	}
}
