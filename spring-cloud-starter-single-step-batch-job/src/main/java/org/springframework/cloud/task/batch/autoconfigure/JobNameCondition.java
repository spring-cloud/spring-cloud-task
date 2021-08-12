/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * Condition to determine if Single Step Job should be enabled.
 *
 * @author Glenn Renfro
 * @since 2.3.4
 */
public class JobNameCondition extends AnyNestedCondition {
	public JobNameCondition() {
		super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnProperty(prefix = "spring.batch.job", name = "jobName")
	static class Value1Condition {

	}

	@ConditionalOnProperty(prefix = "spring.batch.job", name = "job-name")
	static class Value2Condition {

	}

}
