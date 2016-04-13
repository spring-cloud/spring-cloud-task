package io.spring.cloud;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@EnableTask
@EnableBatchProcessing
public class BatchEventsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchEventsApplication.class, args);
	}

	@Configuration
	public static class JobConfiguration {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public Step step1() {
			return this.stepBuilderFactory.get("step1")
					.tasklet(new Tasklet() {
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println("Tasklet has run");
							return RepeatStatus.FINISHED;
						}
					}).build();
		}

		@Bean
		public Step step2() {
			return this.stepBuilderFactory.get("step2")
					.<String, String>chunk(3)
					.reader(new ListItemReader<>(Arrays.asList("1", "2", "3", "4", "5", "6")))
					.processor(new ItemProcessor<String, String>() {
						@Override
						public String process(String item) throws Exception {
							return String.valueOf(Integer.parseInt(item) * -1);
						}
					})
					.writer(new ItemWriter<String>() {
						@Override
						public void write(List<? extends String> items) throws Exception {
							for (String item : items) {
								System.out.println(">> " + item);
							}
						}
					}).build();
		}

		@Bean
		public Job job() {
			return this.jobBuilderFactory.get("job")
					.start(step1())
					.next(step2())
					.build();
		}
	}
}
