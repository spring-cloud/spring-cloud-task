/*
 * Copyright 2015-present the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
@SpringBootTest(
	classes = MultipleDataSourcesApplication.class,
	args = "--spring.profiles.active=embedded"
)
@ExtendWith(OutputCaptureExtension.class)
public class MultiDataSourcesApplicationTests {

	@Autowired
	@Qualifier("secondDataSource")
	private DataSource secondDataSource;

	@Autowired
	private TaskConfigurer taskConfigurer;

	@Autowired
	private PlatformTransactionManager springCloudTaskTransactionManager;

	@Test
	public void testTimeStampApp(CapturedOutput capturedOutput) {

		String output = capturedOutput.toString();

		assertThat(output).contains("There are 2 DataSources within this application");
		assertThat(output).contains("Creating: TaskExecution{");
		assertThat(output).contains("Updating: TaskExecution");
	}

	@Test
	public void shouldConfigureTaskTransactionManagerWithSecondDataSource() {

		PlatformTransactionManager transactionManager = this.taskConfigurer.getTransactionManager();

		assertThat(this.springCloudTaskTransactionManager)
			.isSameAs(transactionManager);

		assertThat(transactionManager)
			.isInstanceOf(DataSourceTransactionManager.class);

		DataSourceTransactionManager dataSourceTransactionManager =
			(DataSourceTransactionManager) transactionManager;

		assertThat(dataSourceTransactionManager.getDataSource())
			.isSameAs(this.secondDataSource);
	}

}
