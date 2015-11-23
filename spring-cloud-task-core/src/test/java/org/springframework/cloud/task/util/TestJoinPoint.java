/*
 *
 *  * Copyright 2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.task.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;

/**
 * Stubbed out join point for testing purposes.
 *
 * @author Glenn Renfro
 */
public class TestJoinPoint implements JoinPoint {
	public String toShortString() {
		return null;
	}

	public String toLongString() {
		return null;
	}

	public Object getThis() {
		return null;
	}

	public Object getTarget() {
		return new TaskBasic();
	}

	public Object[] getArgs() {
		return new Object[0];
	}

	public Signature getSignature() {
		return null;
	}

	public SourceLocation getSourceLocation() {
		return null;
	}

	public String getKind() {
		return null;
	}

	public StaticPart getStaticPart() {
		return null;
	}
}
