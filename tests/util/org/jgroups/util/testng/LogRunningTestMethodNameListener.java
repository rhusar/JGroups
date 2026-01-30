package org.jgroups.util.testng;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 * @author Radoslav Husar
 */
public class LogRunningTestMethodNameListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            System.out.println("Running: " + method.getTestMethod().getRealClass().getName() + "#" + method.getTestMethod().getMethodName());
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            System.out.println(method.getTestMethod().getRealClass().getName() + "#" + method.getTestMethod().getMethodName() + ": " + (testResult.isSuccess() ? "PASS" : "FAIL"));
        }
    }
}