/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.task.timestamp;


import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Glenn Renfro
 */
public class TimestampTaskPropertiesTests {

	@Test
	public void testEmptyFormat() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues testPropertyValues = TestPropertyValues.of("format:");
		testPropertyValues.applyTo(context);
		context.register(Conf.class);
		context.refresh();
		TimestampTaskProperties properties = context
			.getBean(TimestampTaskProperties.class);
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			properties.getFormat();
		});
	}

	@Test
	public void testFormatDefault() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Conf.class);
		context.refresh();
		TimestampTaskProperties properties = context
			.getBean(TimestampTaskProperties.class);
		assertThat(properties.getFormat()).as("result does not match default format.")
			.isEqualTo("yyyy-MM-dd HH:mm:ss.SSS");
	}

	@Test
	public void testFormatSet() {
		final String FORMAT = "yyyy";
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Conf.class);
		context.refresh();
		TimestampTaskProperties properties = context
			.getBean(TimestampTaskProperties.class);
		properties.setFormat(FORMAT);
		assertThat(properties.getFormat()).as("result does not match established format.")
			.isEqualTo(FORMAT);
	}

	@Configuration
	@EnableConfigurationProperties(TimestampTaskProperties.class)
	static class Conf {
	}
}
