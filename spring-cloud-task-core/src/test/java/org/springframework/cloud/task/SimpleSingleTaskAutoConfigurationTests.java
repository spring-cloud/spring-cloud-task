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

package org.springframework.cloud.task;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.task.configuration.SingleTaskConfiguration;
import org.springframework.cloud.task.configuration.SimpleTaskConfiguration;
import org.springframework.cloud.task.configuration.SingleInstanceTaskListener;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the beans created by the SimpleSingleTaskAutoConfigurationConfiguration
 * specifically that PassThruRegistry was selected.
 *
 * @author Glenn Renfro
 * @since 2.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TaskProperties.class, SimpleTaskConfiguration.class, SingleTaskConfiguration.class})
@TestPropertySource(properties = {
		"spring.cloud.task.singleInstanceEnabled=true",
})
public class SimpleSingleTaskAutoConfigurationTests {
	@Autowired
	private ConfigurableApplicationContext context;

	@Test
	public void testConfiguration() throws Exception {

		SingleInstanceTaskListener singleInstanceTaskListener = this.context.getBean(SingleInstanceTaskListener.class);

		assertNotNull("singleInstanceTaskListener should not be null", singleInstanceTaskListener);

		assertEquals(singleInstanceTaskListener.getClass(), SingleInstanceTaskListener.class);

	}

}
