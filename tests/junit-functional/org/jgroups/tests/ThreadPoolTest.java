package org.jgroups.tests;

import org.jgroups.Global;
import org.jgroups.util.ThreadPool;
import org.jgroups.util.Util;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link ThreadPool}
 * @author Radoslav Husar
 */
@Test(groups=Global.FUNCTIONAL)
public class ThreadPoolTest {

    /**
     * A rejection policy of "discard" drops a task without raising an exception; execute() must not report such a
     * task as accepted, or else callers (e.g. MaxOneThreadPerSender) wait forever for a task which will never run
     */
    public void testDiscardingRejectionPolicy() throws Exception {
        ThreadPool pool=create("discard");
        CountDownLatch release=new CountDownLatch(1);
        try {
            saturate(pool, release);
            assert !pool.execute(() -> {}) : "the task was discarded, but execute() returned true";
            assert pool.numberOfRejectedMessages() == 1 : "rejected messages: " + pool.numberOfRejectedMessages();
        }
        finally {
            release.countDown();
            pool.destroy();
        }
    }

    /**
     * A progress check without a fallback policy drops a rejected task just as "discard" does, so it has to be
     * reported to the caller in the same way
     */
    public void testProgressCheckRejectionPolicyWithoutFallback() throws Exception {
        ThreadPool pool=create("progress_check");
        CountDownLatch release=new CountDownLatch(1);
        try {
            saturate(pool, release);
            assert !pool.execute(() -> {}) : "the task was dropped, but execute() returned true";
            assert pool.numberOfRejectedMessages() == 1 : "rejected messages: " + pool.numberOfRejectedMessages();
        }
        finally {
            release.countDown();
            pool.destroy();
        }
    }

    /** A policy which runs the rejected task on the caller's thread has not dropped it: execute() must return true */
    public void testCallerRunsRejectionPolicy() throws Exception {
        ThreadPool pool=create("run");
        CountDownLatch release=new CountDownLatch(1);
        try {
            saturate(pool, release);
            CountDownLatch ran=new CountDownLatch(1);
            assert pool.execute(ran::countDown) : "the task was run by the caller, but execute() returned false";
            assert ran.getCount() == 0;
            assert pool.numberOfRejectedMessages() == 0 : "rejected messages: " + pool.numberOfRejectedMessages();
        }
        finally {
            release.countDown();
            pool.destroy();
        }
    }

    /**
     * In contrast to execute(), doExecute() has no way of reporting a rejection to its caller, so a policy which
     * drops tasks silently has to keep doing so there
     */
    public void testDoExecuteDropsSilently() throws Exception {
        ThreadPool pool=create("discard");
        CountDownLatch release=new CountDownLatch(1);
        try {
            saturate(pool, release);
            pool.doExecute(() -> {}); // must not throw
        }
        finally {
            release.countDown();
            pool.destroy();
        }
    }

    /** A policy which rejects rather than drops still raises an exception in doExecute() */
    public void testDoExecuteWithAbortingPolicy() throws Exception {
        ThreadPool pool=create("abort");
        CountDownLatch release=new CountDownLatch(1);
        try {
            saturate(pool, release);
            try {
                pool.doExecute(() -> {});
                assert false : "a RejectedExecutionException should have been thrown";
            }
            catch(RejectedExecutionException expected) {
            }
        }
        finally {
            release.countDown();
            pool.destroy();
        }
    }

    /**
     * DiscardOldestPolicy drops the head of the queue and retries; as this pool hands tasks over through a
     * SynchronousQueue, the retry is rejected again and recurses until the stack overflows
     */
    public void testDiscardOldestRejectionPolicyIsRefused() throws Exception {
        try {
            new ThreadPool().setRejectionPolicy("discardoldest");
            assert false : "an IllegalArgumentException should have been thrown";
        }
        catch(IllegalArgumentException expected) {
        }

        // the policy can also be injected into the field directly, bypassing setRejectionPolicy()
        ThreadPool pool=new ThreadPool();
        Util.setField(Util.getField(ThreadPool.class, "rejection_policy"), pool, "discardoldest");
        try {
            pool.init();
            assert false : "an IllegalArgumentException should have been thrown";
        }
        catch(IllegalArgumentException expected) {
        }
        finally {
            pool.destroy();
        }
    }

    /**
     * A shut-down pool rejects every task, but ShutdownRejectedExecutionHandler swallows the rejection rather than
     * raising an exception; execute() must not report such a task as accepted
     */
    public void testExecuteAfterShutdown() throws Exception {
        ThreadPool pool=new ThreadPool();
        pool.init();
        pool.destroy();
        assert !pool.execute(() -> {}) : "the pool is shut down, but execute() returned true";
    }

    protected static ThreadPool create(String rejection_policy) throws Exception {
        ThreadPool pool=new ThreadPool().setMinThreads(1).setMaxThreads(1);
        pool.setRejectionPolicy(rejection_policy);
        pool.init();
        return pool;
    }

    /** Occupies the pool's only thread until {@code release} is counted down, so that the next task is rejected */
    protected static void saturate(ThreadPool pool, CountDownLatch release) throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        assert pool.execute(() -> {
            started.countDown();
            try {release.await(30, TimeUnit.SECONDS);}
            catch(InterruptedException ignored) {}
        });
        assert started.await(10, TimeUnit.SECONDS);
    }
}
