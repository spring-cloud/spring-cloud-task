/*
 * Copyright 2019-present the original author or authors.
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

package org.springframework.cloud.task.batch.autoconfigure.flatfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.task.batch.autoconfigure.RangeConverter;
import org.springframework.context.annotation.Bean;

/**
 * Autconfiguration for a {@code FlatFileItemReader}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties(FlatFileItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class FlatFileItemReaderAutoConfiguration {

	private final FlatFileItemReaderProperties properties;

	public FlatFileItemReaderAutoConfiguration(FlatFileItemReaderProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.flatfileitemreader", name = "name")
	public FlatFileItemReader<Map<String, Object>> itemReader(@Autowired(required = false) LineTokenizer lineTokenizer,
			@Autowired(required = false) FieldSetMapper<Map<String, Object>> fieldSetMapper,
			@Autowired(required = false) LineMapper<Map<String, Object>> lineMapper,
			@Autowired(required = false) LineCallbackHandler skippedLinesCallback,
			@Autowired(required = false) RecordSeparatorPolicy recordSeparatorPolicy) {
		FlatFileItemReaderBuilder<Map<String, Object>> mapFlatFileItemReaderBuilder = new FlatFileItemReaderBuilder<Map<String, Object>>()
			.name(this.properties.getName())
			.resource(this.properties.getResource())
			.saveState(this.properties.isSaveState())
			.maxItemCount(this.properties.getMaxItemCount())
			.currentItemCount(this.properties.getCurrentItemCount())
			.strict(this.properties.isStrict())
			.encoding(this.properties.getEncoding())
			.linesToSkip(this.properties.getLinesToSkip())
			.comments(this.properties.getComments().toArray(new String[this.properties.getComments().size()]));

		mapFlatFileItemReaderBuilder.lineTokenizer(lineTokenizer);
		if (recordSeparatorPolicy != null) {
			mapFlatFileItemReaderBuilder.recordSeparatorPolicy(recordSeparatorPolicy);
		}
		mapFlatFileItemReaderBuilder.fieldSetMapper(fieldSetMapper);
		mapFlatFileItemReaderBuilder.lineMapper(lineMapper);
		mapFlatFileItemReaderBuilder.skippedLinesCallback(skippedLinesCallback);

		if (this.properties.isDelimited()) {
			mapFlatFileItemReaderBuilder.delimited()
				.quoteCharacter(this.properties.getQuoteCharacter())
				.delimiter(this.properties.getDelimiter())
				.includedFields(this.properties.getIncludedFields().toArray(new Integer[0]))
				.names(this.properties.getNames())
				.beanMapperStrict(this.properties.isParsingStrict())
				.fieldSetMapper(new MapFieldSetMapper());
		}
		else if (this.properties.isFixedLength()) {
			RangeConverter rangeConverter = new RangeConverter();
			List<Range> ranges = new ArrayList<>();
			this.properties.getRanges().forEach(range -> ranges.add(rangeConverter.convert(range)));
			mapFlatFileItemReaderBuilder.fixedLength()
				.columns(ranges.toArray(new Range[0]))
				.names(this.properties.getNames())
				.fieldSetMapper(new MapFieldSetMapper())
				.beanMapperStrict(this.properties.isParsingStrict());
		}

		return mapFlatFileItemReaderBuilder.build();
	}

	/**
	 * A {@link FieldSetMapper} that takes a {@code FieldSet} and returns the
	 * {@code Map<String, Object>} of its contents.
	 */
	public static class MapFieldSetMapper implements FieldSetMapper<Map<String, Object>> {

		@Override
		public Map<String, Object> mapFieldSet(FieldSet fieldSet) {
			return new HashMap<String, Object>((Map) fieldSet.getProperties());
		}

	}

}
