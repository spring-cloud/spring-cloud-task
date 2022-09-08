/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.flatfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.batch.autoconfigure.RangeConverter;
import org.springframework.cloud.task.batch.autoconfigure.SingleStepJobAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class FlatFileItemReaderAutoConfigurationTests {

	/**
	 * Contents of the file to be read (included here because it's UTF-16).
	 *
	 * <pre>
	 * 1@2@3@4@5@six
	 * # This should be ignored
	 * 7@8@9@10@11@twelve
	 * $ So should this
	 * 13@14@15@16@17@eighteen
	 * 19@20@21@22@23@%twenty four%
	 * 15@26@27@28@29@thirty
	 * 31@32@33@34@35@thirty six
	 * 37@38@39@40@41@forty two
	 * 43@44@45@46@47@forty eight
	 * 49@50@51@52@53@fifty four
	 * 55@56@57@58@59@sixty
	 * </pre>
	 */
	@Test
	public void testFullDelimitedConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(JobConfiguration.class)
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class, SingleStepJobAutoConfiguration.class,
						FlatFileItemReaderAutoConfiguration.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5", "spring.batch.job.flatfileitemreader.savestate=true",
						"spring.batch.job.flatfileitemreader.name=fullDelimitedConfiguration",
						"spring.batch.job.flatfileitemreader.maxItemCount=5",
						"spring.batch.job.flatfileitemreader.currentItemCount=2",
						"spring.batch.job.flatfileitemreader.comments=#,$",
						"spring.batch.job.flatfileitemreader.resource=/testUTF16.csv",
						"spring.batch.job.flatfileitemreader.strict=true",
						"spring.batch.job.flatfileitemreader.encoding=UTF-16",
						"spring.batch.job.flatfileitemreader.linesToSkip=1",
						"spring.batch.job.flatfileitemreader.delimited=true",
						"spring.batch.job.flatfileitemreader.delimiter=@",
						"spring.batch.job.flatfileitemreader.quoteCharacter=%",
						"spring.batch.job.flatfileitemreader.includedFields=1,3,5",
						"spring.batch.job.flatfileitemreader.names=foo,bar,baz",
						"spring.batch.job.flatfileitemreader.parsingStrict=false");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(3);
			assertThat(((Map) writtenItems.get(0)).get("foo")).isEqualTo("20");
			assertThat(((Map) writtenItems.get(0)).get("bar")).isEqualTo("22");
			assertThat(((Map) writtenItems.get(0)).get("baz")).isEqualTo("twenty four");
			assertThat(((Map) writtenItems.get(1)).get("foo")).isEqualTo("26");
			assertThat(((Map) writtenItems.get(1)).get("bar")).isEqualTo("28");
			assertThat(((Map) writtenItems.get(1)).get("baz")).isEqualTo("thirty");
			assertThat(((Map) writtenItems.get(2)).get("foo")).isEqualTo("32");
			assertThat(((Map) writtenItems.get(2)).get("bar")).isEqualTo("34");
			assertThat(((Map) writtenItems.get(2)).get("baz")).isEqualTo("thirty six");
		});
	}

	@Test
	public void testFixedWidthConfiguration() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(JobConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class, FlatFileItemReaderAutoConfiguration.class,
								RangeConverter.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5", "spring.batch.job.flatfileitemreader.savestate=true",
						"spring.batch.job.flatfileitemreader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfileitemreader.comments=#,$",
						"spring.batch.job.flatfileitemreader.resource=/test.txt",
						"spring.batch.job.flatfileitemreader.strict=true",
						"spring.batch.job.flatfileitemreader.fixedLength=true",
						"spring.batch.job.flatfileitemreader.ranges=3-4,7-8,11",
						"spring.batch.job.flatfileitemreader.names=foo,bar,baz",
						"spring.batch.job.flatfileitemreader.parsingStrict=false");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(6);
			assertThat(((Map) writtenItems.get(0)).get("foo")).isEqualTo("2");
			assertThat(((Map) writtenItems.get(0)).get("bar")).isEqualTo("4");
			assertThat(((Map) writtenItems.get(0)).get("baz")).isEqualTo("six");
			assertThat(((Map) writtenItems.get(1)).get("foo")).isEqualTo("8");
			assertThat(((Map) writtenItems.get(1)).get("bar")).isEqualTo("10");
			assertThat(((Map) writtenItems.get(1)).get("baz")).isEqualTo("twelve");
			assertThat(((Map) writtenItems.get(2)).get("foo")).isEqualTo("14");
			assertThat(((Map) writtenItems.get(2)).get("bar")).isEqualTo("16");
			assertThat(((Map) writtenItems.get(2)).get("baz")).isEqualTo("eighteen");
			assertThat(((Map) writtenItems.get(3)).get("foo")).isEqualTo("20");
			assertThat(((Map) writtenItems.get(3)).get("bar")).isEqualTo("22");
			assertThat(((Map) writtenItems.get(3)).get("baz")).isEqualTo("twenty four");
			assertThat(((Map) writtenItems.get(4)).get("foo")).isEqualTo("26");
			assertThat(((Map) writtenItems.get(4)).get("bar")).isEqualTo("28");
			assertThat(((Map) writtenItems.get(4)).get("baz")).isEqualTo("thirty");
			assertThat(((Map) writtenItems.get(5)).get("foo")).isEqualTo("32");
			assertThat(((Map) writtenItems.get(5)).get("bar")).isEqualTo("34");
			assertThat(((Map) writtenItems.get(5)).get("baz")).isEqualTo("thirty six");
		});
	}

	@Test
	public void testCustomLineMapper() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomLineMapperConfiguration.class)
				.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
						BatchAutoConfiguration.class, SingleStepJobAutoConfiguration.class,
						FlatFileItemReaderAutoConfiguration.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfileitemreader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfileitemreader.resource=/test.txt",
						"spring.batch.job.flatfileitemreader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(8);
		});
	}

	/**
	 * This test requires an input file with an even number of records.
	 */
	@Test
	public void testCustomRecordSeparatorAndSkippedLines() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(RecordSeparatorAndSkippedLinesJobConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class, FlatFileItemReaderAutoConfiguration.class,
								RangeConverter.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfileitemreader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfileitemreader.resource=/test.txt",
						"spring.batch.job.flatfileitemreader.linesToSkip=2",
						"spring.batch.job.flatfileitemreader.fixedLength=true",
						"spring.batch.job.flatfileitemreader.ranges=3-4,7-8,11",
						"spring.batch.job.flatfileitemreader.names=foo,bar,baz",
						"spring.batch.job.flatfileitemreader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			ListLineCallbackHandler callbackHandler = context.getBean(ListLineCallbackHandler.class);

			assertThat(callbackHandler.getLines().size()).isEqualTo(2);

			List writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(2);
		});
	}

	@Test
	public void testCustomMapping() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CustomMappingConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class, BatchAutoConfiguration.class,
								SingleStepJobAutoConfiguration.class, FlatFileItemReaderAutoConfiguration.class,
								RangeConverter.class, DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.batch.job.jobName=job", "spring.batch.job.stepName=step1",
						"spring.batch.job.chunkSize=5",
						"spring.batch.job.flatfileitemreader.name=fixedWidthConfiguration",
						"spring.batch.job.flatfileitemreader.resource=/test.txt",
						"spring.batch.job.flatfileitemreader.maxItemCount=1",
						"spring.batch.job.flatfileitemreader.strict=true");

		applicationContextRunner.run((context) -> {
			JobLauncher jobLauncher = context.getBean(JobLauncher.class);

			Job job = context.getBean(Job.class);

			ListItemWriter itemWriter = context.getBean(ListItemWriter.class);

			JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

			JobExplorer jobExplorer = context.getBean(JobExplorer.class);

			while (jobExplorer.getJobExecution(jobExecution.getJobId()).isRunning()) {
				Thread.sleep(1000);
			}

			List<Map<String, Object>> writtenItems = itemWriter.getWrittenItems();

			assertThat(writtenItems.size()).isEqualTo(1);
			assertThat(writtenItems.get(0).get("one")).isEqualTo("1 2 3");
			assertThat(writtenItems.get(0).get("two")).isEqualTo("4 5 six");
		});
	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomMappingConfiguration {

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

		@Bean
		public LineTokenizer lineTokenizer() {
			return line -> new DefaultFieldSet(new String[] { line.substring(0, 5), line.substring(6) },
					new String[] { "one", "two" });
		}

		@Bean
		public FieldSetMapper<Map<String, Object>> fieldSetMapper() {
			return fieldSet -> new HashMap<String, Object>((Map) fieldSet.getProperties());
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class JobConfiguration {

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class RecordSeparatorAndSkippedLinesJobConfiguration {

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public RecordSeparatorPolicy recordSeparatorPolicy() {
			return new RecordSeparatorPolicy() {
				@Override
				public boolean isEndOfRecord(String record) {

					boolean endOfRecord = false;

					int index = record.indexOf('\n');

					if (index > 0 && record.length() > index + 1) {
						endOfRecord = true;
					}

					return endOfRecord;
				}

				@Override
				public String postProcess(String record) {
					return record;
				}

				@Override
				public String preProcess(String record) {
					return record + '\n';
				}
			};
		}

		@Bean
		public LineCallbackHandler lineCallbackHandler() {
			return new ListLineCallbackHandler();
		}

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	@EnableBatchProcessing
	@Configuration
	public static class CustomLineMapperConfiguration {

		@Bean
		public PlatformTransactionManager platformTransactionManager() {
			return new ResourcelessTransactionManager();
		}

		@Bean
		public LineMapper<Map<String, Object>> lineMapper() {
			return (line, lineNumber) -> {
				Map<String, Object> item = new HashMap<>(1);

				item.put("line", line);
				item.put("lineNumber", lineNumber);

				return item;
			};
		}

		@Bean
		public ListItemWriter<Map<String, Object>> itemWriter() {
			return new ListItemWriter<>();
		}

	}

	public static class ListLineCallbackHandler implements LineCallbackHandler {

		private List<String> lines = new ArrayList<>();

		@Override
		public void handleLine(String line) {
			lines.add(line);
		}

		public List<String> getLines() {
			return lines;
		}

	}

}
