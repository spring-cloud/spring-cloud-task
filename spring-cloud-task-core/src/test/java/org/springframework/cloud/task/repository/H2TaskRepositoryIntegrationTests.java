package org.springframework.cloud.task.repository;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Henning Pöttker
 */
class H2TaskRepositoryIntegrationTests {

	static Stream<Arguments> testTaskRepository() {
		return Arrays.stream(org.h2.engine.Mode.ModeEnum.values())
			.map(mode -> Arguments.of(mode.toString()));
	}

	@ParameterizedTest
	@MethodSource
	void testTaskRepository(String compatibilityMode) {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("test.compatibility-mode=" + compatibilityMode);
		applicationContextRunner.run((context -> {
			TaskExplorer taskExplorer = context.getBean(TaskExplorer.class);
			assertThat(taskExplorer.getTaskExecutionCount()).isOne();
		}));
	}

	@EnableTask
	@ImportAutoConfiguration(SimpleTaskAutoConfiguration.class)
	static class TestConfiguration {
		@Bean
		DataSource dataSource(@Value("${test.compatibility-mode}") String compatibilityMode) {
			String connectionUrl = String.format(
				"jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=%s",
				UUID.randomUUID(),
				compatibilityMode
			);
			return new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		}

		@Bean
		ApplicationRunner applicationRunner() {
			return args -> {};
		}
	}

}
