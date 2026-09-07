package configuration.listeners;

import helpers.utils.TestExportUtil;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestExportListener implements ITestListener {

    @Override
    public void onTestSuccess(ITestResult result) {
        TestExportUtil.recordTestResultIfMissing(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        TestExportUtil.recordTestResultIfMissing(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        TestExportUtil.recordTestResultIfMissing(result);
    }
}
