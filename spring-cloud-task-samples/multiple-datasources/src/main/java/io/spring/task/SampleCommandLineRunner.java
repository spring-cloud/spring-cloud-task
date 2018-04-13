/*
 * Copyright 2018 the original author or authors.
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
package io.spring.task;

import java.util.List;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component
public class SampleCommandLineRunner implements CommandLineRunner {

	private List<DataSource> dataSources;

	@Autowired
	public SampleCommandLineRunner(List<DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("There are " + dataSources.size() +
				" DataSources within this application");
	}
}
