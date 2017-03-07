package org.springframework.cloud.task.batch.listener.support;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 *  @author Ali Shahbour
 */
@ConfigurationProperties(prefix = "spring.cloud.task.events")
public class TaskEventProperties {

    private ListenerProperties jobExecution = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties stepExecution = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties itemRead = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties itemProcess = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties itemWrite = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties chunk = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);
    private ListenerProperties skip = new ListenerProperties(Ordered.HIGHEST_PRECEDENCE);

    public ListenerProperties getJobExecution() {
        return jobExecution;
    }

    public void setJobExecution(ListenerProperties jobExecution) {
        this.jobExecution = jobExecution;
    }

    public ListenerProperties getStepExecution() {
        return stepExecution;
    }

    public void setStepExecution(ListenerProperties stepExecution) {
        this.stepExecution = stepExecution;
    }

    public ListenerProperties getItemRead() {
        return itemRead;
    }

    public void setItemRead(ListenerProperties itemRead) {
        this.itemRead = itemRead;
    }

    public ListenerProperties getItemProcess() {
        return itemProcess;
    }

    public void setItemProcess(ListenerProperties itemProcess) {
        this.itemProcess = itemProcess;
    }

    public ListenerProperties getItemWrite() {
        return itemWrite;
    }

    public void setItemWrite(ListenerProperties itemWrite) {
        this.itemWrite = itemWrite;
    }

    public ListenerProperties getChunk() {
        return chunk;
    }

    public void setChunk(ListenerProperties chunk) {
        this.chunk = chunk;
    }

    public ListenerProperties getSkip() {
        return skip;
    }

    public void setSkip(ListenerProperties skip) {
        this.skip = skip;
    }

    public  class ListenerProperties {


        public ListenerProperties(int order) {
            this.order = order;
        }

        private int order;

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }
}
