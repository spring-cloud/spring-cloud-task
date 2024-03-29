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

package org.springframework.cloud.task.repository.database.support;

import org.springframework.data.domain.Pageable;

/**
 * IBM DB2 implementation of a
 * {@link org.springframework.cloud.task.repository.database.PagingQueryProvider} using
 * database specific features.
 *
 * @author Thomas Schuettel
 * @author Ryan DCruz
 */
public class Db2PagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String getPageQuery(Pageable pageable) {
		long offset = pageable.getOffset() + 1;
		return generateRowNumSqlQueryWithNesting(getSelectClause(), false,
				"TMP_ROW_NUM >= " + offset + " AND TMP_ROW_NUM < " + (offset + pageable.getPageSize()));
	}

	private String generateRowNumSqlQueryWithNesting(String selectClause, boolean remainingPageQuery,
			String rowNumClause) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ")
			.append(selectClause)
			.append(" FROM (SELECT ")
			.append(selectClause)
			.append(", ")
			.append("ROW_NUMBER() OVER() as TMP_ROW_NUM");
		sql.append(" FROM (SELECT ").append(selectClause).append(" FROM ").append(this.getFromClause());
		SqlPagingQueryUtils.buildWhereClause(this, remainingPageQuery, sql);
		sql.append(" ORDER BY ").append(SqlPagingQueryUtils.buildSortClause(this));
		sql.append(")) WHERE ").append(rowNumClause);

		return sql.toString();
	}

}
