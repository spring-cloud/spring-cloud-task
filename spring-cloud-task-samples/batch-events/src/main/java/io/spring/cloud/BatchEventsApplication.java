/*
 * Copyright 2016-present the original author or authors.
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

package io.spring.cloud;

import java.util.Arrays;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@EnableTask
@SpringBootApplication
public class BatchEventsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchEventsApplication.class, args);
	}

	@Configuration
	public static class JobConfiguration {

		private static final int DEFAULT_CHUNK_COUNT = 3;

		@Autowired
		private JobRepository jobRepository;

		@Autowired
		private PlatformTransactionManager transactionManager;

		@Bean
		public Step step1() {
			return new StepBuilder("step1", this.jobRepository).tasklet(new Tasklet() {
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					System.out.println("Tasklet has run");
					return RepeatStatus.FINISHED;
				}
			}, transactionManager).build();
		}

		@Bean
		public Step step2() {
			return new StepBuilder("step2", this.jobRepository)
				.<String, String>chunk(DEFAULT_CHUNK_COUNT, this.transactionManager)
				.reader(new ListItemReader<>(Arrays.asList("1", "2", "3", "4", "5", "6")))
				.processor(new ItemProcessor<String, String>() {
					@Override
					public String process(String item) throws Exception {
						return String.valueOf(Integer.parseInt(item) * -1);
					}
				})
				.writer(new ItemWriter<String>() {
					@Override
					public void write(Chunk<? extends String> items) throws Exception {
						for (String item : items) {
							System.out.println(">> " + item);
						}
					}
				})
				.build();
		}

		@Bean
		public Job job() {
			return new JobBuilder("job", this.jobRepository).start(step1()).next(step2()).build();
		}

	}

}
