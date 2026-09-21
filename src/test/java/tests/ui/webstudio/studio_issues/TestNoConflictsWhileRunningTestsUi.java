package tests.ui.webstudio.studio_issues;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TestNoConflictsWhileRunningTestsUi extends BaseTest {

    private static final String TEMPLATE = "Example 3 - Auto Policy Calculation";
    private static final String MODULE = "AutoPolicyCalculation";

    @Test
    @TestCaseId("EPBDS-16636")
    @Description("Running the tests of a module asks the server for the results without being refused and "
            + "without a 409 reaching the browser log, which is the fix of EPBDS-16636 this test guards.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRunningTestsIsNotRefusedWithConflict() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        List<String> refusals = Collections.synchronizedList(new ArrayList<>());
        Page page = DriverPool.getPage();
        Consumer<Response> watcher = response -> {
            if (response.status() == 409 && response.url().contains("/tests/")) {
                refusals.add(response.request().method() + " " + response.url());
            }
        };
        page.onResponse(watcher);

        editorPage.getEditorToolbarPanelComponent().runAllTests();
        int tablesRun = editorPage.getTestResultValidationComponent().countTestTables();
        page.offResponse(watcher);

        assertThat(tablesRun)
                .as("Precondition: the tests of the module must have run")
                .isPositive();
        assertThat(refusals)
                .as("Reading the results of the tests the reader ran should not be refused as a conflict")
                .isEmpty();
    }
}
