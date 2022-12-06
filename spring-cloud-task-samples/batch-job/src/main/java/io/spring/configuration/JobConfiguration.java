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

package io.spring.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael Minella
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(JobConfiguration.RuntimeHint.class)
public class JobConfiguration {

	private static final Log logger = LogFactory.getLog(JobConfiguration.class);

	@Autowired
	public JobRepository jobRepository;

	@Autowired
	public PlatformTransactionManager transactionManager;

	@Bean
	public Job job1() {
		return new JobBuilder("job1", this.jobRepository)
				.start(new StepBuilder("job1step1", this.jobRepository).tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						logger.info("Job1 was run");
						return RepeatStatus.FINISHED;
					}
				}, transactionManager).build()).build();
	}

	static class RuntimeHint implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			hints.proxies().registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.launch.JobOperator"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class));
		}

	}

}
