/*
 * Copyright 2015 the original author or authors.
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

package demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.task.annotation.Task;
import org.springframework.stereotype.Component;

/**
 * A commandline runner that prints hello world and is annotated with @Task.
 * This task is does not set its taskname and the name thus demo.SampleTask will appear
 * in the logs and repository.
 */
@Task
@Component
public class SampleTask implements CommandLineRunner {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public void run(String... args) {
		logger.info("Hello World");
	}

}
