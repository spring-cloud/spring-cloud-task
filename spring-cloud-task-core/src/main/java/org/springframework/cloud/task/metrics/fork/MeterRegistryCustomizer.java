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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Forked from the
 * org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer form
 * spring-boot-actuator
 *
 * Callback interface that can be used to customize auto-configured {@link MeterRegistry
 * MeterRegistries}.
 * <p>
 * Customizers are guaranteed to be applied before any {@link Meter} is registered with
 * the registry.
 *
 * @author Michael Minella
 */
@FunctionalInterface
public interface MeterRegistryCustomizer<T extends MeterRegistry> {

	/**
	 * Customize the given {@code registry}.
	 * @param registry the registry to customize
	 */
	void customize(T registry);

}
