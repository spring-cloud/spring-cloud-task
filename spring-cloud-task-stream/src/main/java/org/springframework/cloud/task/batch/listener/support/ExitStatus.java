/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.task.batch.listener.support;

import org.springframework.util.Assert;

/**
 * ExitStatus DTO created so that {@link org.springframework.batch.core.ExitStatus} can be serialized into Json without
 * having to add mixins to an ObjectMapper
 * @author Glenn Renfro
 */
public class ExitStatus {

	private String exitCode;

	private String exitDescription;

	public ExitStatus(){
	}

	public ExitStatus(org.springframework.batch.core.ExitStatus exitStatus) {
		Assert.notNull(exitStatus, "exitStatus must not be null.");

		this.exitCode = exitStatus.getExitCode();
		this.exitDescription = exitStatus.getExitDescription();
	}

	public String getExitCode() {
		return exitCode;
	}

	public void setExitCode(String exitCode) {
		this.exitCode = exitCode;
	}

	public String getExitDescription() {
		return exitDescription;
	}

	public void setExitDescription(String exitDescription) {
		this.exitDescription = exitDescription;
	}
}
