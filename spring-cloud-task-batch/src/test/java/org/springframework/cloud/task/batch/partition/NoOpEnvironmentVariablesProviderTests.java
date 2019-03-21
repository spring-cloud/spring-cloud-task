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

package org.springframework.cloud.task.batch.partition;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class NoOpEnvironmentVariablesProviderTests {

	private NoOpEnvironmentVariablesProvider provider;

	@Before
	public void setUp() {
		this.provider = new NoOpEnvironmentVariablesProvider();
	}

	@Test
	public void test() {
		Map<String, String> environmentVariables = this.provider
				.getEnvironmentVariables(null);
		assertThat(environmentVariables).isNotNull();
		assertThat(environmentVariables.isEmpty()).isTrue();

		Map<String, String> environmentVariables2 = this.provider
				.getEnvironmentVariables(null);
		assertThat(environmentVariables == environmentVariables2).isTrue();
	}

}
