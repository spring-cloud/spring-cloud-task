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
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
	private Map<String, String> environmentProperties;
	private Map<String, String> deploymentProperties;
	private String applicationName;

	/**
	 * Constructor for the TaskLaunchRequest.
	 * @param uri the URI to the task artifact to be launched.
	 * @param commandlineArguments list of commandlineArguments to be used by the task
	 * @param environmentProperties are the environment variables for this task.
	 * @param deploymentProperties are the variables used to setup task on the platform.
	 * @param applicationName name to be applied to the launched task.   If set
	 * 	to null then the launched task name will be "Task-<hash code of the
	 * 	TaskLaunchRequest>.
	 */
	public TaskLaunchRequest(String uri, List<String> commandlineArguments,
							Map<String, String> environmentProperties,
							Map<String, String> deploymentProperties,
							String applicationName) {
		Assert.hasText(uri, "uri must not be empty nor null.");

		this.uri = uri;
		this.commandlineArguments = (commandlineArguments == null) ? new ArrayList<String>() : commandlineArguments;
		this.environmentProperties = environmentProperties == null ? new HashMap<String, String>() : environmentProperties;
		this.deploymentProperties = deploymentProperties == null ? new HashMap<String, String>() : deploymentProperties;
		setApplicationName(applicationName);
	}

	/**
	 * Constructor for the TaskLaunchRequest.
	 *
	 * @since 2.0.0
	 */
	public TaskLaunchRequest() {
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

	public Map<String, String> getEnvironmentProperties() {
		return environmentProperties;
	}

	/**
	 * Returns the properties used by a {@link org.springframework.cloud.deployer.spi.task.TaskLauncher}
	 *
	 * @return deployment properties
	 */
	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	/**
	 * Returns the name that will be associated with the launched task.
	 *
	 * @return string containing the application name.
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Sets the name to be applied to the launched task.   If set
	 * 	to null then the launched task name will be "Task-<unique id>".
	 *
	 * @param applicationName the name to be
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = !StringUtils.hasText(applicationName) ? "Task-" +
				UUID.randomUUID().toString() : applicationName;
	}

	@Override
	public String toString() {
		return "TaskLaunchRequest{" +
				"uri='" + uri + '\'' +
				", commandlineArguments=" + commandlineArguments +
				", environmentProperties=" + environmentProperties +
				", deploymentProperties=" + deploymentProperties +
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
		if (!commandlineArguments.equals(that.commandlineArguments)){
			return false;
		}
		if(!deploymentProperties.equals(that.deploymentProperties))
		{
			return false;
		}
		return environmentProperties.equals(that.environmentProperties);

	}

	@Override
	public int hashCode() {
		int result = uri.hashCode();
		result = 31 * result + commandlineArguments.hashCode();
		result = 31 * result + environmentProperties.hashCode();
		result = 31 * result + deploymentProperties.hashCode();
		return result;
	}
}
