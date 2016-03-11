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
import java.util.HashMap;
import java.util.Map;

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
	private String artifact;
	private String taskGroupId;
	private String taskVersion;
	private String taskExtension;
	private String taskClassifier;
	private Map<String, String> properties;

	/**
	 * Constructor for the TaskLaunchRequest;
	 * @param artifact is maven artifact coordinate for the task. Must not be empty nor null.
	 * @param taskGroupId is maven groupId coordinate for the task. Must not be empty nor null.
	 * @param taskVersion is maven version coordinate for the task. Must not be empty nor null.
	 * @param taskExtension is maven extension coordinate for the task.
	 * @param taskClassifier is maven classifier coordinate for the task.
	 * @param properties is the environment variables for this task.
	 */
	public TaskLaunchRequest(String artifact, String taskGroupId, String taskVersion,
							 String taskExtension, String taskClassifier,
							 Map<String, String> properties) {
		Assert.hasText(artifact, "artifact must not be empty nor null.");
		Assert.hasText(taskGroupId, "taskGroupID must not be empty nor null.");
		Assert.hasText(taskVersion, "taskVersion must not be empty nor null.");
		Assert.hasText(taskExtension, "taskExtension must not be empty nor null.");

		this.artifact = artifact;
		this.taskGroupId = taskGroupId;
		this.taskVersion = taskVersion;
		this.taskExtension = taskExtension;
		this.taskClassifier = taskClassifier;
		this.properties = properties == null ? new HashMap() : properties;
	}

	/**
	 * Retrieves the group maven coordinate for the task.
 	 * @return group maven coordinate for the task.
	 */
	public String getTaskGroupId() {
		return taskGroupId;
	}

	/**
	 * Retrieves the version maven coordinate for the task.
	 * @return version maven coordinate for the task.
	 */
	public String getTaskVersion() {
		return taskVersion;
	}

	/**
	 * Retrieves the extension maven coordinate for the task.
	 * @return extension maven coordinate for the task.
	 */
	public String getTaskExtension() {
		return taskExtension;
	}

	/**
	 * Retrieves the classifier maven coordinate for the task.
	 * @return classifier maven coordinate for the task.
	 */
	public String getTaskClassifier() {
		return taskClassifier;
	}

	/**
	 * Retrieves the artifact maven coordinate for the task.
	 * @return artifact maven coordinate for the task.
	 */
	public String getArtifact() {
		return artifact;
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
		String coordinates = taskGroupId + ":" + artifact + ":" + taskVersion ;
		if(StringUtils.hasText(taskClassifier)){
			coordinates = coordinates + ":" + taskClassifier;
		}
		coordinates = coordinates + ":" + taskExtension;
		return coordinates;
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

		if (!artifact.equals(that.artifact)){
			return false;
		}
		if (!taskGroupId.equals(that.taskGroupId)){
			return false;
		}
		if (!taskVersion.equals(that.taskVersion)){
			return false;
		}
		if (!taskExtension.equals(that.taskExtension)){
			return false;
		}
		if (taskClassifier != null ? !taskClassifier.equals(that.taskClassifier) : that.taskClassifier != null){
			return false;
		}
		return properties != null ? properties.equals(that.properties) : that.properties == null;

	}

	@Override
	public int hashCode() {
		int result = artifact != null ? artifact.hashCode() : 0;
		result = 31 * result + (taskGroupId != null ? taskGroupId.hashCode() : 0);
		result = 31 * result + (taskVersion != null ? taskVersion.hashCode() : 0);
		result = 31 * result + (taskExtension != null ? taskExtension.hashCode() : 0);
		result = 31 * result + (taskClassifier != null ? taskClassifier.hashCode() : 0);
		result = 31 * result + (properties != null ? properties.hashCode() : 0);
		return result;
	}
}
