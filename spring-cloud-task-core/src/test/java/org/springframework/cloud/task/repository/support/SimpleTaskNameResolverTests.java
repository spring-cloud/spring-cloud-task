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

package org.springframework.cloud.task.repository.support;

import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class SimpleTaskNameResolverTests {

	@Test
	public void testDefault() {
		GenericApplicationContext context = new GenericApplicationContext();

		SimpleTaskNameResolver taskNameResolver = new SimpleTaskNameResolver();
		taskNameResolver.setApplicationContext(context);

		assertThat(taskNameResolver.getTaskName().startsWith(
				"org.springframework.context.support.GenericApplicationContext"))
						.isTrue();
	}

	@Test
	public void testWithProfile() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setId("foo:bar");

		SimpleTaskNameResolver taskNameResolver = new SimpleTaskNameResolver();
		taskNameResolver.setApplicationContext(context);

		assertThat(taskNameResolver.getTaskName().startsWith("foo_bar")).isTrue();
	}

	@Test
	public void testApplicationName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setId("foo");

		SimpleTaskNameResolver taskNameResolver = new SimpleTaskNameResolver();
		taskNameResolver.setApplicationContext(context);

		assertThat(taskNameResolver.getTaskName()).isEqualTo("foo");
	}

	@Test
	public void testExternalConfig() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setId("foo");

		SimpleTaskNameResolver taskNameResolver = new SimpleTaskNameResolver();
		taskNameResolver.setApplicationContext(context);

		taskNameResolver.setConfiguredName("bar");

		assertThat(taskNameResolver.getTaskName()).isEqualTo("bar");
	}

}
