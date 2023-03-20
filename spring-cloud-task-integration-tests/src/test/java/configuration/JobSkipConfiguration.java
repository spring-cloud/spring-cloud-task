/*
 * Copyright 2016-2022 the original author or authors.
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

package configuration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Glenn Renfro
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.task.test", name = "enable-fail-job-configuration")
public class JobSkipConfiguration {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Bean
	public Job job() {
		return new JobBuilder("job", this.jobRepository).start(step1()).next(step2()).build();
	}

	@Bean
	public Step step1() {
		return new StepBuilder("step1", this.jobRepository).tasklet(new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				System.out.println("Executed");
				return RepeatStatus.FINISHED;
			}
		}, transactionManager).build();
	}

	@Bean
	public Step step2() {
		return new StepBuilder("step2", this.jobRepository).chunk(3, transactionManager).faultTolerant()
				.skip(IllegalStateException.class).skipLimit(100).reader(new SkipItemReader())
				.processor(new ItemProcessor<Object, Object>() {
					@Override
					public String process(Object item) throws Exception {
						return String.valueOf(Integer.parseInt((String) item) * -1);
					}
				}).writer(new SkipItemWriter()).build();
	}

}
