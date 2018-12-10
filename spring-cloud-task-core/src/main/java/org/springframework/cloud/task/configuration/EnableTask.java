/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * Enables the {@link org.springframework.cloud.task.listener.TaskLifecycleListener}
 * so that the features of Spring Cloud Task will be applied.
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTask
 * public class AppConfig {
 *
 * 	&#064;Bean
 * 	public MyCommandLineRunner myCommandLineRunner() {
 * 		return new MyCommandLineRunner()
 * 	}
 * }
 * </pre>
 *
 * Note that only one of your configuration classes needs to have the <code>&#064;EnableTask</code>
 * annotation. Once you have an <code>&#064;EnableTask</code> class in your configuration
 * the task will have the Spring Cloud Task features available.
 *
 * @author Glenn Renfro
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(TaskLifecycleConfiguration.class)
public @interface EnableTask {
}
