/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.task.metrics.fork;

import java.util.List;
import java.util.stream.Collectors;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forked and stripped down version of the
 * org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration form
 * spring-boot-actuator
 *
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Michael Minella
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Timed.class)
@AutoConfigureBefore(name = {
		"org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration" })
public class MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClockFork() {
		return Clock.SYSTEM;
	}

	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessorFork(
			ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers) {
		return new MeterRegistryPostProcessor(meterRegistryCustomizers);
	}

	/**
	 * Forked and stripped down version of the
	 * org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryPostProcessor
	 * form spring-boot-actuator
	 *
	 * {@link BeanPostProcessor} that delegates to a lazily created
	 * {@link MeterRegistryConfigurer} to post-process {@link MeterRegistry} beans.
	 *
	 */
	static class MeterRegistryPostProcessor implements BeanPostProcessor {

		private final ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers;

		private volatile MeterRegistryConfigurer configurer;

		MeterRegistryPostProcessor(
				ObjectProvider<MeterRegistryCustomizer<?>> meterRegistryCustomizers) {
			this.meterRegistryCustomizers = meterRegistryCustomizers;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof MeterRegistry) {
				getConfigurer().configure((MeterRegistry) bean);
			}
			return bean;
		}

		private MeterRegistryConfigurer getConfigurer() {
			if (this.configurer == null) {
				this.configurer = new MeterRegistryConfigurer(
						this.meterRegistryCustomizers);
			}
			return this.configurer;
		}

	}

	/**
	 * Forked and stripped down version of the
	 * org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryConfigurer form
	 * spring-boot-actuator
	 *
	 * Configurer to apply {@link MeterRegistryCustomizer customizers} to
	 * {@link MeterRegistry meter registries}.
	 *
	 */
	static class MeterRegistryConfigurer {

		private final ObjectProvider<MeterRegistryCustomizer<?>> customizers;

		MeterRegistryConfigurer(ObjectProvider<MeterRegistryCustomizer<?>> customizers) {
			this.customizers = customizers;
		}

		void configure(MeterRegistry registry) {
			customize(registry);
		}

		@SuppressWarnings("unchecked")
		private void customize(MeterRegistry registry) {
			LambdaSafe
					.callbacks(MeterRegistryCustomizer.class,
							asOrderedList(this.customizers), registry)
					.withLogger(MeterRegistryConfigurer.class)
					.invoke((customizer) -> customizer.customize(registry));
		}

		private <T> List<T> asOrderedList(ObjectProvider<T> provider) {
			return provider.orderedStream().collect(Collectors.toList());
		}

	}

}
