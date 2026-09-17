package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRunTableResultUi extends BaseTest {

    private static final String WORKBOOK = "RunTableProject.xlsx";
    private static final String MODULE = "RunTableProject";
    private static final String RUN_TABLE = "RunTable";

    @Test
    @TestCaseId("EPBDS-16635")
    @Description("A Run table run from the editor reports what its runs returned. Fails on EPBDS-16635: the "
            + "window of results is empty.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16635")
    public void testRunTableReportsItsResults() {
        String projectName = StringUtil.generateUniqueName("RunTable");
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.EXCEL_FILES, projectName, WORKBOOK);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getProblemsPanelComponent().checkNoProblems();
        editorPage.getEditorLeftRulesTreeComponent().selectVisibleLeafNode(RUN_TABLE);

        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();

        TestResultValidationComponent results = new TestResultValidationComponent();
        assertThat(results.rowsTheRunReported())
                .as("A Run table should report the value each of its runs returned, and the window shows none")
                .isPositive();
    }
}
