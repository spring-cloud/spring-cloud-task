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
package io.spring;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class TaskEventsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskEventsApplication.class, args);
	}

	@Configuration
	public static class TaskConfiguration {

		@Bean
		public CommandLineRunner commandLineRunner() {
			return new CommandLineRunner() {
				@Override
				public void run(String... args) throws Exception {
					System.out.println("The CommandLineRunner was executed");
				}
			};
		}
	}
}
