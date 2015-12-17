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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.database.Order;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.database.PagingQueryProvider;
import org.springframework.cloud.task.util.TestDBUtils;

/**
 * @author Glenn Renfro
 */
public class SqlPagingQueryProviderFactoryBeanTests {

	private SqlPagingQueryProviderFactoryBean factoryBean;

	@Before
	public void setup() throws Exception{
		factoryBean = new SqlPagingQueryProviderFactoryBean();
		factoryBean.setDataSource(TestDBUtils.getMockDataSource("MySQL"));
		factoryBean.setDatabaseType("Oracle");
		factoryBean.setSelectClause(JdbcTaskExecutionDao.SELECT_CLAUSE);
		factoryBean.setFromClause(JdbcTaskExecutionDao.FROM_CLAUSE);
		Map<String, Order> orderMap = new TreeMap<>();
		orderMap.put("START_TIME", Order.DESCENDING);
		orderMap.put("TASK_EXECUTION_ID", Order.DESCENDING);
		factoryBean.setSortKeys(orderMap);

	}

	@Test
	public void testDatabaseType() throws Exception{
		PagingQueryProvider pagingQueryProvider = factoryBean.getObject();
		assertThat(pagingQueryProvider, instanceOf(OraclePagingQueryProvider.class));
	}

	@Test
	public void testIsSingleton(){
		assertTrue(factoryBean.isSingleton());
	}

}
