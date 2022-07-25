/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Task Application outputs the correct task log entries.
 *
 * @author Michael Minella
 */
@ExtendWith(OutputCaptureExtension.class)
public class BatchJobApplicationTests {

	@Test
	public void testBatchJobApp(CapturedOutput capturedOutput) throws Exception {
		final String JOB_RUN_MESSAGE = " was run";
		final String CREATE_TASK_MESSAGE = "Creating: TaskExecution{executionId=";
		final String UPDATE_TASK_MESSAGE = "Updating: TaskExecution with executionId=";
		final String JOB_ASSOCIATION_MESSAGE = "The job execution id ";
		final String EXIT_CODE_MESSAGE = "with the following {exitCode=0";

		SpringApplication.run(BatchJobApplication.class);

		String output = capturedOutput.toString();
		assertThat(output).contains(JOB_RUN_MESSAGE);
		assertThat(output).contains(CREATE_TASK_MESSAGE);
		assertThat(output).contains(UPDATE_TASK_MESSAGE);
		assertThat(output).contains(EXIT_CODE_MESSAGE);

		int i = output.indexOf(JOB_ASSOCIATION_MESSAGE);

		assertThat(i).isGreaterThan(0);

		String taskTitle = "Demo Batch Job Task";
		Pattern pattern = Pattern.compile(taskTitle);
		Matcher matcher = pattern.matcher(output);

		int count = 0;
		while (matcher.find()) {
			count++;
		}
		assertThat(count).isEqualTo(1);
	}

}
