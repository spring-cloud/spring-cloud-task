/*
 * Copyright 2020-present the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure;

import org.springframework.batch.infrastructure.item.file.transform.Range;
import org.springframework.core.convert.converter.Converter;

/**
 * Converter for taking properties of format {@code start-end} or {@code start} (where
 * start and end are both integers) and converting them into {@link Range} instances for
 * configuring a
 * {@link org.springframework.batch.infrastructure.item.file.FlatFileItemReader}.
 *
 * @author Michael Minella
 * @since 2.3
 */
public class RangeConverter implements Converter<String, Range> {

	@Override
	public Range convert(String source) {
		if (source == null) {
			return null;
		}

		String[] columns = source.split("-");

		if (columns.length == 1) {
			int start = Integer.parseInt(columns[0]);
			return new Range(start);
		}
		else if (columns.length == 2) {
			int start = Integer.parseInt(columns[0]);
			int end = Integer.parseInt(columns[1]);
			return new Range(start, end);
		}
		else {
			throw new IllegalArgumentException(String
				.format("%s is in an illegal format.  Ranges must be specified as startIndex-endIndex", source));
		}
	}

}
