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

import java.util.Locale;

import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Properties for configuring a {@code FlatFileItemWriter}.
 *
 * @author Michael Minella
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.flatfileitemwriter")
public class FlatFileItemWriterProperties {

	/**
	 * The {@link Resource} to be used as output.
	 */
	private Resource resource;

	/**
	 * Configure the use of the {@code DelimitedLineAggregator} to generate the output per
	 * item. Default is {@code false}.
	 */
	private boolean delimited;

	/**
	 * Indicates to use a {@code FormatterLineAggregator} to generate the output per item.
	 * Default is {@code false}.
	 */
	private boolean formatted;

	/**
	 * Configure the format the {@code FormatterLineAggregator} uses for each item.
	 */
	private String format;

	/**
	 * Configure the {@code Locale} to use when generating the output.
	 */
	private Locale locale = Locale.getDefault();

	/**
	 * Configure the maximum record length. If 0, the size is unbounded.
	 */
	private int maximumLength = 0;

	/**
	 * Configure the minimum record length.
	 */
	private int minimumLength = 0;

	/**
	 * Configure the {@code String} used to delimit the fields in the output file.
	 */
	private String delimiter = ",";

	/**
	 * File encoding for the output file. Defaults to
	 * {@code FlatFileItemWriter.DEFAULT_CHARSET})
	 */
	private String encoding = FlatFileItemWriter.DEFAULT_CHARSET;

	/**
	 * A flag indicating that changes should be force-synced to disk on flush. Defaults to
	 * {@code false}.
	 */
	private boolean forceSync = false;

	/**
	 * Names of the fields to be extracted into the output.
	 */
	private String[] names;

	/**
	 * Configure if the output file is found if it should be appended to. Defaults to
	 * {@code false}.
	 */
	private boolean append = false;

	/**
	 * String used to separate lines in output. Defaults to the {@code System} property
	 * {@code line.separator}.
	 */
	private String lineSeparator = FlatFileItemWriter.DEFAULT_LINE_SEPARATOR;

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #setSaveState} is set to {@code true}.
	 */
	private String name;

	/**
	 * Returns the configured value of whether the state of the reader is persisted.
	 */
	private boolean saveState = true;

	/**
	 * Indicates whether the output file should be deleted if no output was written to it.
	 * Defaults to {@code false}.
	 */
	private boolean shouldDeleteIfEmpty = false;

	/**
	 * Indicates whether an existing output file should be deleted on startup. Defaults to
	 * {@code true}.
	 */
	private boolean shouldDeleteIfExists = true;

	/**
	 * Indicates whether flushing the buffer should be delayed while a transaction is
	 * active. Defaults to {@code true}.
	 */
	private boolean transactional = FlatFileItemWriter.DEFAULT_TRANSACTIONAL;

	/**
	 * Returns the configured value of if the state of the reader will be persisted.
	 * @return true if the state will be persisted
	 */
	public boolean isSaveState() {
		return this.saveState;
	}

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted within
	 * the {@link org.springframework.batch.item.ExecutionContext} for restart purposes.
	 * @param saveState defaults to true
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * Returns the configured value of the name used to calculate {@code ExecutionContext}
	 * keys.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #setSaveState} is set to true.
	 * @param name name of the reader instance
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The output file for the {@code FlatFileItemWriter}.
	 * @return a {@code Resource}
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * The {@link Resource} to be used as output.
	 * @param resource the input to the reader.
	 * @see FlatFileItemWriter#setResource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Indicates of the output will be delimited by a configured string (, by default).
	 * @return true if the output file is a delimited file
	 */
	public boolean isDelimited() {
		return delimited;
	}

	/**
	 * Configure the use of the {@code DelimitedLineAggregator} to generate the output per
	 * item.
	 * @param delimited indicator if the file will be delimited or not
	 */
	public void setDelimited(boolean delimited) {
		this.delimited = delimited;
	}

	/**
	 * When a file is delimited, this {@code String} will be used as the delimiter between
	 * fields.
	 * @return delimiter
	 */
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * Configure the {@code String} used to delimit the fields in the output file.
	 * @param delimiter {@code String} used to delimit the fields of the output file.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Names of the fields to be extracted into the output.
	 * @return An array of field names
	 */
	public String[] getNames() {
		return names;
	}

	/**
	 * Provide an ordered array of field names used to generate the output of a file.
	 * @param names An array of field names
	 */
	public void setNames(String[] names) {
		this.names = names;
	}

	/**
	 * True if an output file is found and should be added onto instead of
	 * replaced/deleted. False by default.
	 * @return appending indicator
	 */
	public boolean isAppend() {
		return append;
	}

	/**
	 * Configure if the output file is found if it should be appended to. Defaults to
	 * false.
	 * @param append true if the output file should be appended onto if found.
	 */
	public void setAppend(boolean append) {
		this.append = append;
	}

	/**
	 * Indicates that the output file will use String formatting to generate the output.
	 * @return true if the file will contain formatted records defaults to true
	 */
	public boolean isFormatted() {
		return formatted;
	}

	/**
	 * Indicates to use a {@code FormatterLineAggregator} to generate the output per item.
	 * @param formatted true if the output should be formatted via the
	 * {@code FormatterLineAggregator}
	 */
	public void setFormatted(boolean formatted) {
		this.formatted = formatted;
	}

	/**
	 * File encoding for the output file.
	 * @return the configured encoding for the output file (Defaults to
	 * {@code FlatFileItemWriter.DEFAULT_CHARSET})
	 */
	public String getEncoding() {
		return encoding;
	}

	/**
	 * Configure encoding of the output file.
	 * @param encoding output encoding
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush. Defaults to
	 * false.
	 * @return The current instance of the builder.
	 */
	public boolean isForceSync() {
		return forceSync;
	}

	/**
	 * A flag indicating that changes should be force-synced to disk on flush. Defaults to
	 * false.
	 * @param forceSync value to set the flag to
	 */
	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	/**
	 * String used to separate lines in output. Defaults to the System property
	 * line.separator.
	 * @return the separator string
	 */
	public String getLineSeparator() {
		return lineSeparator;
	}

	/**
	 * Configure the {@code String} used to separate each line.
	 * @param lineSeparator defaults to System's line.separator property
	 */
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	/**
	 * Indicates if the output file should be deleted if no output was written to it.
	 * Defaults to false.
	 * @return true if a file that is empty at the end of the step should be deleted.
	 */
	public boolean isShouldDeleteIfEmpty() {
		return shouldDeleteIfEmpty;
	}

	/**
	 * Configure if an empty output file should be deleted once the step is complete.
	 * Defaults to false.
	 * @param shouldDeleteIfEmpty true if the file should be deleted if no items have been
	 * written to it.
	 */
	public void setShouldDeleteIfEmpty(boolean shouldDeleteIfEmpty) {
		this.shouldDeleteIfEmpty = shouldDeleteIfEmpty;
	}

	/**
	 * Indicates if an existing output file should be deleted on startup. Defaults to
	 * true.
	 * @return if an existing output file should be deleted.
	 */
	public boolean isShouldDeleteIfExists() {
		return shouldDeleteIfExists;
	}

	/**
	 * Configures if an existing output file should be deleted on the start of the step.
	 * Defaults to true.
	 * @param shouldDeleteIfExists if true and an output file of a previous run is found,
	 * it will be deleted.
	 */
	public void setShouldDeleteIfExists(boolean shouldDeleteIfExists) {
		this.shouldDeleteIfExists = shouldDeleteIfExists;
	}

	/**
	 * Indicates if flushing the buffer should be delayed while a transaction is active.
	 * Defaults to true.
	 * @return flag indicating if flushing should be delayed during a transaction
	 */
	public boolean isTransactional() {
		return transactional;
	}

	/**
	 * Configure if output should not be flushed to disk during an active transaction.
	 * @param transactional defaults to true
	 */
	public void setTransactional(boolean transactional) {
		this.transactional = transactional;
	}

	/**
	 * Format used with the {@code FormatterLineAggregator}.
	 * @return the format for each item's output.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Configure the format the {@code FormatterLineAggregator} will use for each item.
	 * @param format the format for each item's output.
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * The {@code Locale} used when generating the output file.
	 * @return configured {@code Locale}. Defaults to {@code Locale.getDefault()}
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Configure the {@code Locale} to use when generating the output.
	 * @param locale the configured {@code Locale}
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * The longest a record is allowed to be. If 0, the maximum is unlimited.
	 * @return the max record length allowed. Defaults to 0.
	 */
	public int getMaximumLength() {
		return maximumLength;
	}

	/**
	 * Configure the maximum record length. If 0, the size is unbounded.
	 * @param maximumLength the maximum record length allowed.
	 */
	public void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	/**
	 * The minimum record length.
	 * @return the minimum record length allowed.
	 */
	public int getMinimumLength() {
		return minimumLength;
	}

	/**
	 * Configure the minimum record length.
	 * @param minimumLength the minimum record length.
	 */
	public void setMinimumLength(int minimumLength) {
		this.minimumLength = minimumLength;
	}

}
