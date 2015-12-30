/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.task.repository.database.support;

import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.data.domain.Pageable;

/**
 * MySQL implementation of a {@link PagingQueryProvider} using database specific features.
 *
 * @author Glenn Renfro
 */
public class MySqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {
	@Override
	public String getPageQuery(Pageable pageable) {
		String topClause = new StringBuilder().append("LIMIT ")
				.append(pageable.getOffset()).append(", ")
				.append(pageable.getPageSize()).toString();
		return SqlPagingQueryUtils.generateLimitJumpToQuery(this, topClause);
	}

}
