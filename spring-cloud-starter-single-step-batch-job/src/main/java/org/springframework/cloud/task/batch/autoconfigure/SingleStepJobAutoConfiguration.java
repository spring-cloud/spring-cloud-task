/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure;

import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Autoconfiguration to create a single step Spring Batch Job.
 *
 * @author Michael Minella
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties(SingleStepJobProperties.class)
@AutoConfigureBefore(BatchAutoConfiguration.class)
public class SingleStepJobAutoConfiguration {

	private SingleStepJobProperties properties;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	JobRepository jobRepository;

	@Autowired(required = false)
	private ItemProcessor<Map<String, Object>, Map<String, Object>> itemProcessor;

	public SingleStepJobAutoConfiguration(SingleStepJobProperties properties, ApplicationContext context) {

		validateProperties(properties);
		this.properties = properties;
	}

	private void validateProperties(SingleStepJobProperties properties) {
		Assert.hasText(properties.getJobName(), "A job name is required");
		Assert.hasText(properties.getStepName(), "A step name is required");
		Assert.notNull(properties.getChunkSize(), "A chunk size is required");
		Assert.isTrue(properties.getChunkSize() > 0, "A chunk size greater than zero is required");
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job", name = "job-name")
	public Job job(ItemReader<Map<String, Object>> itemReader, ItemWriter<Map<String, Object>> itemWriter) {

		SimpleStepBuilder<Map<String, Object>, Map<String, Object>> stepBuilder = new StepBuilder(
				this.properties.getStepName(), this.jobRepository)
						.<Map<String, Object>, Map<String, Object>>chunk(this.properties.getChunkSize(),
								this.transactionManager)
						.reader(itemReader);

		stepBuilder.processor(this.itemProcessor);

		Step step = stepBuilder.writer(itemWriter).build();

		return new JobBuilder(this.properties.getJobName(), this.jobRepository).start(step).build();
	}

}
