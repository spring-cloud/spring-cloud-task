package org.springframework.cloud.task.repository;

import java.util.UUID;

import javax.sql.DataSource;

import org.h2.engine.Mode.ModeEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henning Pöttker
 */
class H2TaskRepositoryIntegrationTests {

	@ParameterizedTest
	@EnumSource(ModeEnum.class)
	void testTaskRepository(ModeEnum mode) {
		String connectionUrl = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;MODE=%s", UUID.randomUUID(), mode);
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withBean(DataSource.class,
				() -> new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", ""));

		applicationContextRunner.run((context -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isOne();
		}));
	}

	@EnableTask
	@ImportAutoConfiguration(SimpleTaskAutoConfiguration.class)
	static class TestConfiguration {
	}

}
