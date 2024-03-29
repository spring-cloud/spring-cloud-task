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

package io.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
@ExtendWith(OutputCaptureExtension.class)
public class MultiDataSourcesApplicationTests {

	@Test
	public void testTimeStampApp(CapturedOutput capturedOutput) throws Exception {

		SpringApplication.run(MultipleDataSourcesApplication.class, "--spring.profiles.active=embedded");

		String output = capturedOutput.toString();

		assertThat(output.contains("There are 2 DataSources within this application"))
			.as("Unable to find CommandLineRunner output: " + output)
			.isTrue();
		assertThat(output.contains("Creating: TaskExecution{")).as("Unable to find start task message: " + output)
			.isTrue();
		assertThat(output.contains("Updating: TaskExecution")).as("Unable to find update task message: " + output)
			.isTrue();
	}

}
