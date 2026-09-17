package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import configuration.appcontainer.AppContainerPool;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.LogsUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class TestClickOnErrorFromTheBottom extends BaseTest {

    @Test
    @TestCaseId("EPBDS-9309")
    @Description("Test clicking on error from the bottom problems panel by index - Playwright version")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16662")
    public void testClickOnErrorFromTheBottom() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN,
                "TestClickOnErrorFromTheBottom.zip");
        EditorPage editorPage = new EditorPage();

        List<String> serverErrors = Collections.synchronizedList(new ArrayList<>());
        Page page = DriverPool.getPage();
        Consumer<Response> watcher = response -> {
            if (response.status() >= 500 && response.url().contains("/tables")) {
                serverErrors.add(response.status() + " " + response.url());
            }
        };
        page.onResponse(watcher);
        try {
            editorPage.getEditorLeftProjectModuleSelectorComponent()
                    .selectModule(projectName, "ContextDatatypes");
        } finally {
            page.offResponse(watcher);
            assertThat(serverErrors)
                    .as("Listing the tables of a module that holds a table without a body must not answer a "
                            + "server error")
                    .isEmpty();
        }

        editorPage.getProblemsPanelComponent().selectProblemByIndex(1);
        
        assertThat(editorPage.isStudioMessageDisplayed("Sorry! Something went wrong."))
                .as("'Something went wrong' message should not be displayed")
                .isFalse();

        assertThat(editorPage.getEditorMainContentProblemsPanelComponent().isErrorMessageListPresent())
                .as("Error message should be present in top problems panel")
                .isTrue();

        LogsUtil.inspectLogFile(AppContainerPool.get());
    }
}