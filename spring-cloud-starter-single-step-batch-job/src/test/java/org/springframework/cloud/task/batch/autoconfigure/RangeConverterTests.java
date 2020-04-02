/*
 * Copyright 2020-2020 the original author or authors.
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

import org.junit.Test;

import org.springframework.batch.item.file.transform.Range;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael Minella
 */
public class RangeConverterTests {

	@Test
	public void testNullInput() {
		RangeConverter converter = new RangeConverter();

		assertThat(converter.convert(null)).isNull();
	}

	@Test
	public void testStartValueOnly() {
		RangeConverter converter = new RangeConverter();

		Range range = converter.convert("5");

		assertThat(range.getMin()).isEqualTo(5);
		assertThat(range.getMax()).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	public void testStartAndEndValue() {
		RangeConverter converter = new RangeConverter();

		Range range = converter.convert("5-25");

		assertThat(range.getMin()).isEqualTo(5);
		assertThat(range.getMax()).isEqualTo(25);
	}

	@Test(expected = NumberFormatException.class)
	public void testIllegalValue() {
		RangeConverter converter = new RangeConverter();

		converter.convert("invalid");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooManyValues() {
		RangeConverter converter = new RangeConverter();

		converter.convert("1-2-3-4");
	}

}
