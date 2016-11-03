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

	private static final String DEFAULT_URI = "maven://org.springframework.cloud.task.app:"
			+ "timestamp-task:jar:1.0.1.RELEASE";


	private String uri = DEFAULT_URI;

	private String dataSourceUrl;

	private String dataSourceDriverClassName;

	private String dataSourceUserName;

	private String dataSourcePassword;

	private String applicationName;



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

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
}
