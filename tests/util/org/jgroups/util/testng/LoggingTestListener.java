package org.jgroups.util.testng;

import java.util.logging.Logger;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class LoggingTestListener implements ITestListener {

    private static final Logger LOG = Logger.getLogger(LoggingTestListener.class.getPackage().getName());

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("RUNNING " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.severe("FAIL " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warning("SKIP " + result.getTestClass().getName() + "#" + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    }

    @Override
    public void onStart(ITestContext context) {
    }

    @Override
    public void onFinish(ITestContext context) {
    }
}
