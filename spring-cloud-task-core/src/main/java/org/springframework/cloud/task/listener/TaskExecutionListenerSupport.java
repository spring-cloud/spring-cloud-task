/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.task.listener;

/**
 * A no-op implementation of the {@link TaskExecutionListener} to allow for overriding
 * only the methods of interest.
 *
 * @author Michael Minella
 * @since 1.2
 *
 * {@link TaskExecutionListener}
 * @deprecated since 3.0 in favor of the default implementations of
 */
@Deprecated
public class TaskExecutionListenerSupport implements TaskExecutionListener {

}
