package org.jgroups.tests;

import org.jgroups.Global;
import org.jgroups.util.ThreadPool;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
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
