/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.task.launcher.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.cloud.task.launcher.TaskLauncherSink;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * Enable this boot app to be a sink to receive a {@link TaskLaunchRequest} and use the
 * {@link TaskLauncher} to launch the task.
 * </p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTaskLauncher
 * public class AppConfig {
 *
 * 	&#064;Bean
 * 	public MyCommandLineRunner myCommandLineRunner() {
 * 		return new MyCommandLineRunner()
 * 	}
 * }
 * </pre>
 *
 * Note that only one of your configuration classes needs to have the <code>&#064;EnableTaskLauncher</code>
 * annotation.
 *
 * @author Glenn Renfro
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({TaskLauncherSink.class})
public @interface EnableTaskLauncher {
}
