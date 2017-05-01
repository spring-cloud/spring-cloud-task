/*
 *  Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.spring.configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.listener.annotation.BeforeTask;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.stereotype.Component;

/**
 * Records an entry in the TASK_RUN_OUTPUT table on the BeforeTask event.
 * 
 * @author Glenn Renfro
 * @author Pas Apicella
 */
@Component
public class TaskRunComponent {

	private static final Log logger = LogFactory.getLog(TaskRunComponent.class);

	@Autowired
	private TaskRunRepository taskRunRepository;

	@BeforeTask
	public void init(TaskExecution taskExecution) {
		String execDate = new SimpleDateFormat().format(new Date());
		taskRunRepository.save(new TaskRunOutput("Executed at " + execDate));
		logger.info("Executed at : " + execDate);
	}
}
