package org.springframework.cloud.task.batch.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.cloud.task.batch.listener.support.BatchJobHeaders;
import org.springframework.cloud.task.batch.listener.support.MessagePublisher;
import org.springframework.core.Ordered;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 *  Provides informational messages around the {@link Chunk} of a batch job.
 *
 *  The {@link ChunkListener#beforeChunk()} and
 *  {@link ChunkListener#afterChunk(Object)} are both no-ops in this implementation.
 *  {@link ChunkListener#afterChunkError(Object)}.
 *
 * @author Ali Shahbour
 */
public class EventEmittingChunkListener implements ChunkListener, Ordered {

    private static final Log logger = LogFactory.getLog(EventEmittingChunkListener.class);

    private MessagePublisher<String> messagePublisher;
    private int order = Ordered.HIGHEST_PRECEDENCE;

    public EventEmittingChunkListener(MessageChannel output) {
        Assert.notNull(output, "An output channel is required");
        this.messagePublisher = new MessagePublisher(output);
    }

    public EventEmittingChunkListener(MessageChannel output, int order) {
        Assert.notNull(output, "An output channel is required");
        this.messagePublisher = new MessagePublisher(output);
        this.order = order;
    }

    @Override
    public void beforeChunk(ChunkContext context) {

    }

    @Override
    public void afterChunk(ChunkContext context) {

    }

    @Override
    public void afterChunkError(ChunkContext context) {

    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
