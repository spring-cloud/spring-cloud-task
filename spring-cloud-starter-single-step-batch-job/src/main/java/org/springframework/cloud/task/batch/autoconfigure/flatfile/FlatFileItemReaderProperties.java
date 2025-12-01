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
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Properties to configure a {@code FlatFileItemReader}.
 *
 * @author Michael Minella
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.batch.job.flatfileitemreader")
public class FlatFileItemReaderProperties {

	/**
	 * Determines whether the state of the reader is persisted. Default is {@code true}.
	 */
	private boolean saveState = true;

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.infrastructure.item.ExecutionContext}. Required if
	 * {@link #setSaveState} is set to {@code true}.
	 */
	private @Nullable String name;

	/**
	 * Configure the maximum number of items to be read.
	 */
	private int maxItemCount = Integer.MAX_VALUE;

	/**
	 * Index for the current item. Also used on restarts to indicate where to start from.
	 */
	private int currentItemCount = 0;

	/**
	 * A list of {@code String} elements used to indicate which records are comments.
	 */
	private List<String> comments = new ArrayList<>();

	/**
	 * The {@link Resource} to be used as input.
	 */
	private @Nullable Resource resource;

	/**
	 * Configure whether the reader should be in strict mode (require the input
	 * {@link Resource} to exist).
	 */
	private boolean strict = true;

	/**
	 * Configure the encoding used by the reader to read the input source. The default
	 * value is {@link FlatFileItemReader#DEFAULT_CHARSET}.
	 */
	private String encoding = FlatFileItemReader.DEFAULT_CHARSET;

	/**
	 * The number of lines to skip at the beginning of reading the file.
	 */
	private int linesToSkip = 0;

	/**
	 * Indicates that a {@link DelimitedLineTokenizer} should be used to parse each line.
	 */
	private boolean delimited = false;

	/**
	 * Define the delimiter for the file.
	 */
	private String delimiter = DelimitedLineTokenizer.DELIMITER_COMMA;

	/**
	 * Define the character used to quote fields.
	 */
	private char quoteCharacter = DelimitedLineTokenizer.DEFAULT_QUOTE_CHARACTER;

	/**
	 * A list of indices of the fields within a delimited file to be included.
	 */
	private List<Integer> includedFields = new ArrayList<>();

	/**
	 * Indicates that a
	 * {@link org.springframework.batch.infrastructure.item.file.transform.FixedLengthTokenizer}
	 * should be used to parse the records in the file.
	 */
	private boolean fixedLength = false;

	/**
	 * The column ranges to be used to parse a fixed width file.
	 */
	private List<String> ranges = new ArrayList<>();

	/**
	 * The names of the fields to be parsed from the file.
	 */
	private String @Nullable [] names;

	/**
	 * Indicates whether the number of tokens must match the number of configured fields.
	 */
	private boolean parsingStrict = true;

	/**
	 * Returns the configured value of if the state of the reader will be persisted.
	 * @return true if the state will be persisted
	 */
	public boolean isSaveState() {
		return this.saveState;
	}

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.infrastructure.item.ItemStreamSupport} should be
	 * persisted within the
	 * {@link org.springframework.batch.infrastructure.item.ExecutionContext} for restart
	 * purposes.
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
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.infrastructure.item.ExecutionContext}. Required if
	 * {@link #setSaveState} is set to true.
	 * @param name name of the reader instance
	 * @see org.springframework.batch.infrastructure.item.ItemStreamSupport#setName(String)
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * The maximum number of items to be read.
	 * @return the configured number of items, defaults to Integer.MAX_VALUE
	 */
	public int getMaxItemCount() {
		return this.maxItemCount;
	}

	/**
	 * Configure the max number of items to be read.
	 * @param maxItemCount the max items to be read
	 * @see org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader#setMaxItemCount(int)
	 */
	public void setMaxItemCount(int maxItemCount) {
		this.maxItemCount = maxItemCount;
	}

	/**
	 * Provides the index of the current item.
	 * @return item index
	 */
	public int getCurrentItemCount() {
		return this.currentItemCount;
	}

	/**
	 * Index for the current item. Also used on restarts to indicate where to start from.
	 * @param currentItemCount current index
	 * @see org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader#setCurrentItemCount(int)
	 */
	public void setCurrentItemCount(int currentItemCount) {
		this.currentItemCount = currentItemCount;
	}

	/**
	 * List of {@code String} values used to indicate what records are comments.
	 * @return list of comment indicators
	 */
	public List<String> getComments() {
		return this.comments;
	}

	/**
	 * Takes a list of {@code String} elements used to indicate what records are comments.
	 * @param comments strings used to indicate commented lines
	 */
	public void setComments(List<String> comments) {
		this.comments = comments;
	}

	/**
	 * The input file for the {@code FlatFileItemReader}.
	 * @return a Resource
	 */
	public @Nullable Resource getResource() {
		return this.resource;
	}

	/**
	 * The {@link Resource} to be used as input.
	 * @param resource the input to the reader.
	 * @see FlatFileItemReader#setResource(Resource)
	 */
	public void setResource(@Nullable Resource resource) {
		this.resource = resource;
	}

	/**
	 * Returns true if a missing input file is considered an error.
	 * @return true if the input file is required.
	 */
	public boolean isStrict() {
		return this.strict;
	}

	/**
	 * Configure if the reader should be in strict mode (require the input
	 * {@link Resource} to exist).
	 * @param strict true if the input file is required to exist.
	 * @see FlatFileItemReader#setStrict(boolean)
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	/**
	 * Returns the encoding for the input file. Defaults to
	 * {@code FlatFileItemReader#DEFAULT_CHARSET}.
	 * @return the configured encoding
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Configure the encoding used by the reader to read the input source. Default value
	 * is {@link FlatFileItemReader#DEFAULT_CHARSET}.
	 * @param encoding to use to read the input source.
	 * @see FlatFileItemReader#setEncoding(String)
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Number of lines to skip when reading the input file.
	 * @return number of lines
	 */
	public int getLinesToSkip() {
		return this.linesToSkip;
	}

	/**
	 * The number of lines to skip at the beginning of reading the file.
	 * @param linesToSkip number of lines to be skipped.
	 * @see FlatFileItemReader#setLinesToSkip(int)
	 */
	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
	}

	/**
	 * Indicates if the input file is a delimited file or not.
	 * @return true if the file is delimited
	 */
	public boolean isDelimited() {
		return this.delimited;
	}

	/**
	 * Indicates that a {@link DelimitedLineTokenizer} should be used to parse each line.
	 * @param delimited true if the file is a delimited file
	 */
	public void setDelimited(boolean delimited) {
		this.delimited = delimited;
	}

	/**
	 * The {@code String} used to divide the record into fields.
	 * @return the delimiter
	 */
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * Define the delimiter for the file.
	 * @param delimiter String used as a delimiter between fields.
	 * @see DelimitedLineTokenizer#setDelimiter(String)
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * The char used to indicate that a field is quoted.
	 * @return the quote char
	 */
	public char getQuoteCharacter() {
		return this.quoteCharacter;
	}

	/**
	 * Define the character used to quote fields.
	 * @param quoteCharacter char used to define quoted fields
	 * @see DelimitedLineTokenizer#setQuoteCharacter(char)
	 */
	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
	}

	/**
	 * A {@code List} of indices indicating what fields to include.
	 * @return list of indices
	 */
	public List<Integer> getIncludedFields() {
		return this.includedFields;
	}

	/**
	 * A list of indices of the fields within a delimited file to be included.
	 * @param includedFields indices of the fields
	 * @see DelimitedLineTokenizer#setIncludedFields(int[])
	 */
	public void setIncludedFields(List<Integer> includedFields) {
		this.includedFields = includedFields;
	}

	/**
	 * Indicates that a file contains records with fixed length columns.
	 * @return true if the file is parsed using column indices
	 */
	public boolean isFixedLength() {
		return this.fixedLength;
	}

	/**
	 * Indicates that a
	 * {@link org.springframework.batch.infrastructure.item.file.transform.FixedLengthTokenizer}
	 * should be used to parse the records in the file.
	 * @param fixedLength true if the records should be tokenized by column index
	 */
	public void setFixedLength(boolean fixedLength) {
		this.fixedLength = fixedLength;
	}

	/**
	 * The column ranges to be used to parsed a fixed width file.
	 * @return a list of {@link Range} instances
	 */
	public List<String> getRanges() {
		return this.ranges;
	}

	/**
	 * Column ranges for each field.
	 * @param ranges list of ranges in start-end format (end is optional)
	 */
	public void setRanges(List<String> ranges) {
		this.ranges = ranges;
	}

	/**
	 * Names of each column.
	 * @return names
	 */
	public String @Nullable [] getNames() {
		return this.names;
	}

	/**
	 * The names of the fields to be parsed from the file.
	 * @param names names of fields
	 */
	public void setNames(String @Nullable [] names) {
		this.names = names;
	}

	/**
	 * Indicates if the number of tokens must match the number of configured fields.
	 * @return true if they must match
	 */
	public boolean isParsingStrict() {
		return this.parsingStrict;
	}

	/**
	 * Indicates if the number of tokens must match the number of configured fields.
	 * @param parsingStrict true if they must match
	 */
	public void setParsingStrict(boolean parsingStrict) {
		this.parsingStrict = parsingStrict;
	}

}
