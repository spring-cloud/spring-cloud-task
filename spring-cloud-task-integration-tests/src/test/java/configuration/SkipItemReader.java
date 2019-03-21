/*
 * Copyright 2016-2019 the original author or authors.
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

package configuration;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * @author Glenn Renfro
 */
public class SkipItemReader implements ItemReader {

	int failCount = 0;

	boolean finished = false;

	@Override
	public Object read() throws Exception, UnexpectedInputException, ParseException,
			NonTransientResourceException {
		String result = "1";
		if (this.failCount < 2) {
			this.failCount++;
			throw new IllegalStateException("Reader FOOBAR");
		}
		if (this.finished) {
			result = null;
		}
		this.finished = true;
		return result;
	}

}
