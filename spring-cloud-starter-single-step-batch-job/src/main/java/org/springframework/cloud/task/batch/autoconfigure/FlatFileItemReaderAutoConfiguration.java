/*
 * Copyright 2019-2020 the original author or authors.
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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autconfiguration for a {@code FlatFileItemReader}.
 *
 * @author Michael Minella
 * @since 2.3
 */
@Configuration
@EnableConfigurationProperties(FlatFileItemReaderProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class FlatFileItemReaderAutoConfiguration {

	private final FlatFileItemReaderProperties properties;

	@Autowired(required = false)
	private LineTokenizer lineTokenizer;

	@Autowired(required = false)
	private FieldSetMapper<Map<Object, Object>> fieldSetMapper;

	@Autowired(required = false)
	private LineMapper<Map<Object, Object>> lineMapper;

	@Autowired(required = false)
	private LineCallbackHandler skippedLinesCallback;

	@Autowired(required = false)
	private RecordSeparatorPolicy recordSeparatorPolicy;

	public FlatFileItemReaderAutoConfiguration(FlatFileItemReaderProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.flatfilereader", name = "name")
	public FlatFileItemReader<Map<Object, Object>> itemReader() {
		FlatFileItemReaderBuilder<Map<Object, Object>> mapFlatFileItemReaderBuilder = new FlatFileItemReaderBuilder<Map<Object, Object>>()
				.name(this.properties.getName()).resource(this.properties.getResource())
				.saveState(this.properties.isSaveState()).maxItemCount(this.properties.getMaxItemCount())
				.currentItemCount(this.properties.getCurrentItemCount()).strict(this.properties.isStrict())
				.encoding(this.properties.getEncoding()).linesToSkip(this.properties.getLinesToSkip())
				.comments(this.properties.getComments().toArray(new String[this.properties.getComments().size()]));

		if (this.lineTokenizer != null) {
			mapFlatFileItemReaderBuilder.lineTokenizer(this.lineTokenizer);
		}

		if (this.recordSeparatorPolicy != null) {
			mapFlatFileItemReaderBuilder.recordSeparatorPolicy(this.recordSeparatorPolicy);
		}

		if (this.fieldSetMapper != null) {
			mapFlatFileItemReaderBuilder.fieldSetMapper(this.fieldSetMapper);
		}

		if (this.lineMapper != null) {
			mapFlatFileItemReaderBuilder.lineMapper(this.lineMapper);
		}

		if (this.skippedLinesCallback != null) {
			mapFlatFileItemReaderBuilder.skippedLinesCallback(skippedLinesCallback);
		}

		if (this.properties.isDelimited()) {
			mapFlatFileItemReaderBuilder.delimited().quoteCharacter(this.properties.getQuoteCharacter())
					.delimiter(this.properties.getDelimiter())
					.includedFields(this.properties.getIncludedFields().toArray(new Integer[0]))
					.names(this.properties.getNames()).beanMapperStrict(this.properties.isParsingStrict())
					.fieldSetMapper(new MapFieldSetMapper());
		}
		else if (this.properties.isFixedLength()) {
			mapFlatFileItemReaderBuilder.fixedLength().columns(this.properties.getRanges().toArray(new Range[0]))
					.names(this.properties.getNames()).fieldSetMapper(new MapFieldSetMapper())
					.beanMapperStrict(this.properties.isParsingStrict());
		}

		return mapFlatFileItemReaderBuilder.build();
	}

	/**
	 * A {@link FieldSetMapper} that takes a {@code FieldSet} and returns the
	 * {@code Map<Object, Object>} of its contents.
	 */
	public static class MapFieldSetMapper implements FieldSetMapper<Map<Object, Object>> {

		@Override
		public Map<Object, Object> mapFieldSet(FieldSet fieldSet) {
			return fieldSet.getProperties();
		}

	}

}
