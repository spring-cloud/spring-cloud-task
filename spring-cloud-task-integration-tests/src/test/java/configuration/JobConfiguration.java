/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Glenn Renfro
 */
@Configuration
@EnableBatchProcessing
@EnableTask
public class JobConfiguration {
	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Job job() {
		return jobBuilderFactory.get("job")
				.start(step1()).next(step2())
				.build();
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").tasklet(new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				System.out.println("Executed");
				return RepeatStatus.FINISHED;
			}
		}).build();
	}

	@Bean
	public Step step2() {
		return stepBuilderFactory.get("step2").<String, String>chunk(3)
				.reader(new ListItemReader<>(Arrays.asList("1", "2", "3", "4", "5", "6")))
				.processor(new ItemProcessor<String, String>() {
					@Override
					public String process(String item) throws Exception {
						return String.valueOf(Integer.parseInt((String) item) * -1);
					}
				})
				.writer(new ItemWriter<String>() {
					@Override
					public void write(List<? extends String> items) throws Exception {
						for (Object item : items) {
							System.out.println(">> " + item);
						}
					}
				}).build();
	}

}
