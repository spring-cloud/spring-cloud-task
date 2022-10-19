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

package org.springframework.cloud.task.batch.autoconfigure.flatfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.WritableResource;

/**
 * Autoconfiguration for a {@code FlatFileItemWriter}.
 *
 * @author Michael Minella
 * @since 2.3
 */
@AutoConfiguration
@EnableConfigurationProperties(FlatFileItemWriterProperties.class)
@AutoConfigureAfter(BatchAutoConfiguration.class)
public class FlatFileItemWriterAutoConfiguration {

	private FlatFileItemWriterProperties properties;

	@Autowired(required = false)
	private LineAggregator<Map<String, Object>> lineAggregator;

	@Autowired(required = false)
	private FieldExtractor<Map<String, Object>> fieldExtractor;

	@Autowired(required = false)
	private FlatFileHeaderCallback headerCallback;

	@Autowired(required = false)
	private FlatFileFooterCallback footerCallback;

	public FlatFileItemWriterAutoConfiguration(FlatFileItemWriterProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.batch.job.flatfileitemwriter", name = "name")
	public FlatFileItemWriter<Map<String, Object>> itemWriter() {

		if (this.properties.isDelimited() && this.properties.isFormatted()) {
			throw new IllegalStateException("An output file must be either delimited or formatted or a custom "
					+ "LineAggregator must be provided. Your current configuration specifies both delimited and formatted");
		}
		else if ((this.properties.isFormatted() || this.properties.isDelimited()) && this.lineAggregator != null) {
			throw new IllegalStateException(
					"A LineAggregator must be configured if the " + "output is not formatted or delimited");
		}

		FlatFileItemWriterBuilder<Map<String, Object>> builder = new FlatFileItemWriterBuilder<Map<String, Object>>()
				.name(this.properties.getName()).resource((WritableResource) this.properties.getResource())
				.append(this.properties.isAppend()).encoding(this.properties.getEncoding())
				.forceSync(this.properties.isForceSync()).lineSeparator(this.properties.getLineSeparator())
				.saveState(this.properties.isSaveState()).shouldDeleteIfEmpty(this.properties.isShouldDeleteIfEmpty())
				.shouldDeleteIfExists(this.properties.isShouldDeleteIfExists())
				.transactional(this.properties.isTransactional()).headerCallback(this.headerCallback)
				.footerCallback(this.footerCallback);

		if (this.properties.isDelimited()) {
			FlatFileItemWriterBuilder.DelimitedBuilder<Map<String, Object>> delimitedBuilder = builder.delimited()
					.delimiter(this.properties.getDelimiter());

			if (this.fieldExtractor != null) {
				delimitedBuilder.fieldExtractor(this.fieldExtractor);
			}
			else {
				delimitedBuilder.fieldExtractor(new MapFieldExtractor(this.properties.getNames()));
			}
		}
		else if (this.properties.isFormatted()) {
			FlatFileItemWriterBuilder.FormattedBuilder<Map<String, Object>> formattedBuilder = builder.formatted()
					.format(this.properties.getFormat()).locale(this.properties.getLocale())
					.maximumLength(this.properties.getMaximumLength())
					.minimumLength(this.properties.getMinimumLength());

			if (this.fieldExtractor != null) {
				formattedBuilder.fieldExtractor(this.fieldExtractor);
			}
			else {
				formattedBuilder.fieldExtractor(new MapFieldExtractor(this.properties.getNames()));
			}
		}
		else if (this.lineAggregator != null) {
			builder.lineAggregator(this.lineAggregator);
		}

		return builder.build();
	}

	/**
	 * A {@code FieldExtractor} that converts a {@code Map<String, Object>} to the ordered
	 * {@code Object[]} required to populate an output record.
	 */
	public static class MapFieldExtractor implements FieldExtractor<Map<String, Object>> {

		private String[] names;

		public MapFieldExtractor(String[] names) {
			this.names = names;
		}

		@Override
		public Object[] extract(Map<String, Object> item) {

			List<Object> fields = new ArrayList<>(item.size());

			for (String name : this.names) {
				fields.add(item.get(name));
			}

			return fields.toArray();
		}

	}

}
