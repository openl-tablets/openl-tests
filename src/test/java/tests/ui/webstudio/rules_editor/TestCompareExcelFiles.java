package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import helpers.service.WorkflowService;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.BaseTest;

public class TestCompareExcelFiles extends BaseTest {

    @Test
    @TestCaseId("IPBQA-28380")
    @Description("Compare Excel files functionality: upload two Excel files, verify tree structure, cell differences and highlighting")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCompareExcelFiles() {
        WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");

        throw new SkipException("KNOWN-ISSUES.md #9: the screen that compares two uploaded Excel files has no "
                + "entry point left in the UI. The More menu item named after it opens the comparison of the "
                + "project against its own revisions, so there is nothing to drive this scenario through. The "
                + "scenario it covered is in the history of this file, to be restored with the entry point.");
    }
}
