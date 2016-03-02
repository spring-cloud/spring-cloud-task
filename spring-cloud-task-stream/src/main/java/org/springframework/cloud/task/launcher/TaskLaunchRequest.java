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

/**
 * Request that contains the resource and property information required by the
 * TaskLauncherSink to launch the task.
 *
 * @author Glenn Renfro
 */
public class TaskLaunchRequest implements Serializable{

	private String artifact;
	private String taskGroupId;
	private String taskVersion;
	private String taskExtension;
	private String taskClassifier;
	private Map<String, String> properties;

	public TaskLaunchRequest(String artifact, String taskGroupId, String taskVersion,
							 String taskExtension, String taskClassifier,
							 Map<String, String> properties) {
		this.artifact = artifact;
		this.taskGroupId = taskGroupId;
		this.taskVersion = taskVersion;
		this.taskExtension = taskExtension;
		this.taskClassifier = taskClassifier;
		this.properties = properties == null ? new HashMap<String,String>() :
				properties;
	}

	public String getTaskGroupId() {
		return taskGroupId;
	}

	public String getTaskVersion() {
		return taskVersion;
	}

	public String getTaskExtension() {
		return taskExtension;
	}

	public String getTaskClassifier() {
		return taskClassifier;
	}

	public String getArtifact() {
		return artifact;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "TaskLaunchRequest{" +
				"taskName='" + artifact + '\'' +
				", taskGroupId='" + taskGroupId + '\'' +
				", taskVersion='" + taskVersion + '\'' +
				", taskExtension='" + taskExtension + '\'' +
				", taskClassifier='" + taskClassifier + '\'' +
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

}
