/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.task.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.cloud.task.batch.listener.support.TaskBatchEventListenerBeanPostProcessor;

/**
 * Registers runtime hints for {@link TaskBatchEventListenerBeanPostProcessor}.
 *
 * @author Henning Pöttker
 * @since 3.0
 */
public class TaskStreamRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(ChunkOrientedTasklet.class, MemberCategory.DECLARED_FIELDS);
	}

}
