/*
 *  Copyright 2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package configuration;

import java.util.*;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * @author Glenn Renfro
 */
public class SkipItemWriter implements ItemWriter {

	int failCount = 0;
	boolean finished = false;

	@Override
	public void write(List items) throws Exception {
		String result = "1";
		if(failCount < 2) {
			failCount++;
			throw new IllegalStateException("Writer FOOBAR");
		}
		for (Object item : items) {
			System.out.println(">> " + item);
		}
	}
}
