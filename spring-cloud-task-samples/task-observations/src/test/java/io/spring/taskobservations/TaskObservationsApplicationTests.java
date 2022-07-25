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

package io.spring.taskobservations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class TaskObservationsApplicationTests {

	@Test
	void contextLoads(CapturedOutput output) {
		String result = output.getAll();
		assertThat(result).contains("spring.cloud.task(TIMER)[application='task-observations-application-58', "
				+ "error='none', service='task-observations-application', "
				+ "spring.cloud.task.execution.id='1', spring.cloud.task.exit.code='0', "
				+ "spring.cloud.task.external.execution.id='unknown', spring.cloud.task.name='taskmetrics', "
				+ "spring.cloud.task.parent.execution.id='unknown', spring.cloud.task.status='success']; "
				+ "count=1.0, total_time=");
	}

}
