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

package io.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Glenn Renfro
 */
@ConfigurationProperties
public class TaskProcessorProperties {

	private static final String DEFAULT_GROUP = "io.spring";

	private static final String DEFAULT_ARTIFACT = "timestamp-task";

	private static final String DEFAULT_VERSION = "1.0.0.BUILD-SNAPSHOT";

	private static final String DEFAULT_EXTENSION = "jar";


	private String group = DEFAULT_GROUP;

	private String artifact = DEFAULT_ARTIFACT;

	private String version = DEFAULT_VERSION;

	private String extension = DEFAULT_EXTENSION;

	private String classifiers;

	private String dataSourceUrl;

	private String dataSourceDriverClassName;

	private String dataSourceUserName;

	private String dataSourcePassword;


	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getClassifiers() {
		return classifiers;
	}

	public void setClassifiers(String classifiers) {
		this.classifiers = classifiers;
	}

	public String getDataSourceUrl() {
		return dataSourceUrl;
	}

	public void setDataSourceUrl(String dataSourceUrl) {
		this.dataSourceUrl = dataSourceUrl;
	}

	public String getDataSourceDriverClassName() {
		return dataSourceDriverClassName;
	}

	public void setDataSourceDriverClassName(String dataSourceDriverClassName) {
		this.dataSourceDriverClassName = dataSourceDriverClassName;
	}

	public String getDataSourceUserName() {
		return dataSourceUserName;
	}

	public void setDataSourceUserName(String dataSourceUserName) {
		this.dataSourceUserName = dataSourceUserName;
	}

	public String getDataSourcePassword() {
		return dataSourcePassword;
	}

	public void setDataSourcePassword(String dataSourcePassword) {
		this.dataSourcePassword = dataSourcePassword;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public String getArtifact() {
		return artifact;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
}
