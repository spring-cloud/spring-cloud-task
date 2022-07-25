/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import org.springframework.beans.factory.annotation.Value;

/**
 * Provides values for the {@link io.micrometer.common.KeyValues} for the task
 * {@link io.micrometer.observation.Observation} when the cloud profile is active.
 *
 * @author Glenn Renfro
 * @since 3.0
 */
public class TaskObservationCloudKeyValues {

	@Value("${vcap.application.org_name:default}")
	private String organizationName;

	@Value("${vcap.application.space_id:unknown}")
	private String spaceId;

	@Value("${vcap.application.space_name:unknown}")
	private String spaceName;

	@Value("${vcap.application.application_name:unknown}")
	private String applicationName;

	@Value("${vcap.application.application_id:unknown}")
	private String applicationId;

	@Value("${vcap.application.application_version:unknown}")
	private String applicationVersion;

	@Value("${vcap.application.instance_index:0}")
	private String instanceIndex;

	public String getOrganizationName() {
		return organizationName;
	}

	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public void setSpaceName(String spaceName) {
		this.spaceName = spaceName;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public String getInstanceIndex() {
		return instanceIndex;
	}

	public void setInstanceIndex(String instanceIndex) {
		this.instanceIndex = instanceIndex;
	}

}
