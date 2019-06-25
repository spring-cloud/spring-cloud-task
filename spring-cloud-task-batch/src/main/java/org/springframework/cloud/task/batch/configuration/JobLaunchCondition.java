/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.task.batch.configuration;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Evaluates if the correct conditions have been met to create a TaskJobLauncher.
 *
 * @author Glenn Renfro
 * @since 2.2.0
 */
public class JobLaunchCondition extends AllNestedConditions {

	public JobLaunchCondition() {
		super(ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnProperty(name = "spring.cloud.task.batch.fail-on-job-failure",
			havingValue = "true")
	static class FailOnJobFailureCondition {

	}

	@ConditionalOnProperty(prefix = "spring.batch.job", name = "enabled",
			havingValue = "true", matchIfMissing = true)
	static class SpringBatchJobCondition {

	}

}
