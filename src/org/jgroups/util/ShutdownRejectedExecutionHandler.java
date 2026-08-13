package org.jgroups.util;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * ShutdownRejectedExecutionHandler is a decorator RejectedExecutionHandler used
 * in all JGroups ThreadPoolExecutor(s). Default RejectedExecutionHandler raises
 * RuntimeException when a task is submitted to ThreadPoolExecutor that has been
 * shutdown. ShutdownRejectedExecutionHandler instead logs only a warning
 * message.
 * <p>
 * Some policies drop a rejected task <em>silently</em> (see {@link Util#discardsSilently(RejectedExecutionHandler)}),
 * ie. without raising a RejectedExecutionException. As callers such as {@link ThreadPool#execute(Runnable)} would
 * otherwise treat a dropped task as accepted, a {@link DiscardedException} is raised on the policy's behalf. Callers
 * which do want a task to be dropped silently (e.g. {@link ThreadPool#doExecute(Runnable)}) catch that exception.
 *
 * @author Vladimir Blagojevic
 * @see ThreadPoolExecutor
 * @see RejectedExecutionHandler
 */
public class ShutdownRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * Raised on behalf of a rejection policy which dropped a task without raising an exception. A discarding policy
     * is picked to make an overloaded pool cheap, so this exception is shared and carries no stack trace: dropping a
     * message must not become more expensive than delivering it.
     */
    public static class DiscardedException extends RejectedExecutionException {
        private static final long serialVersionUID=6011936732638418694L;

        protected DiscardedException() {
            super("task was dropped by the rejection policy");
        }

        /** A shared exception cannot carry a meaningful stack trace anyway */
        @Override
        public Throwable fillInStackTrace() {return this;}
    }

    /** Raised whenever a task is dropped: it is stackless, so a per-task instance would carry no information */
    protected static final DiscardedException DISCARDED=new DiscardedException();

    protected final RejectedExecutionHandler handler;
    protected final boolean                  discards; // true if the handler drops tasks without raising an exception

    public ShutdownRejectedExecutionHandler(RejectedExecutionHandler handler) {
        super();
        this.handler=Objects.requireNonNull(handler);
        this.discards=Util.discardsSilently(handler);
    }

    public RejectedExecutionHandler handler() {return handler;}

    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if(!executor.isShutdown()) {
            handler.rejectedExecution(r, executor);
            if(discards) // the task was dropped silently: tell the caller that it will never be run
                throw DISCARDED;
        }
    }
}
