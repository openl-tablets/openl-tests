package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestTestButtonAvailable extends BaseTest {

    private static final int TABLE_LOAD_TIMEOUT_MS = 30_000;

    private static final String NAME_PROJECT_MY = "MyProject";

    @Test
    @TestCaseId("IPBQA-31701")
    @Description("Project Compilation - Test button available, run tests, copy module, compilation progress for large project")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testTestButtonAvailable() {
        String nameExample3Project = "Example 3";
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, nameExample3Project, "Example 3 - Auto Policy Calculation");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(nameExample3Project, "AutoPolicyCalculation");
        EditorPage editorPageRef = editorPage;
        WaitUtil.waitForCondition(
                () -> editorPageRef.getEditorToolbarPanelComponent().isTestButtonVisible(),
                5000, 500, "Waiting for Test button to become visible"
        );
        editorPage.getEditorToolbarPanelComponent().runAllTests();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "AccidentPremium");
        editorPage.getEditorToolbarPanelComponent().runAllTests();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "AccidentPremium");
        DriverPool.getPage().reload();
        editorPage = new EditorPage();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCountWhenCounted("3"))
                .as("The button should read Test and carry the number of test tables")
                .isEqualTo("3");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestButtonText())
                .as("The button should read Test and carry the number of test tables")
                .isEqualTo("Test3");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCount())
                .as("The button should say there are 3 test tables")
                .isEqualTo("3");

        DriverPool.getPage().reload();
        editorPage = new EditorPage();
        EditorPage editorPageRef2 = editorPage;
        WaitUtil.waitForCondition(
                () -> editorPageRef2.getEditorToolbarPanelComponent().isTestButtonVisible(),
                5000, 500, "Waiting for Test button after full refresh"
        );
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCountWhenCounted("3"))
                .as("The button should read Test and carry the number of test tables after a full refresh")
                .isEqualTo("3");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestButtonText())
                .as("The button should read Test and carry the number of test tables after a full refresh")
                .isEqualTo("Test3");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCount())
                .as("The button should still say there are 3 test tables after a full refresh")
                .isEqualTo("3");

        editorPage.getEditorToolbarPanelComponent().selectProjectBreadcrumbs(nameExample3Project);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(nameExample3Project, "AutoPolicyTests");
        editorPage.copyModuleWorkbook(nameExample3Project, "AutoPolicyTests.xlsx", "AutoPolicyTests2.xlsx");

        editorPage.getEditorToolbarPanelComponent().selectProjectBreadcrumbs(nameExample3Project);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(nameExample3Project, "AutoPolicyTests2");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "DriverPremiumTest");
        waitForTable(editorPage);
        editorPage.getCenterTable().editCell(1, 1, "Test DetermineDriverPremium DriverPremiumTest1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilAppIdle();

        editorPage.reloadPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder("Test", "PolicyPremiumTest");
        waitForTable(editorPage);
        editorPage.getCenterTable().editCell(1, 1, "Test DeterminePolicyPremium PolicyPremiumTest1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilAppIdle();

        editorPage.reloadPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder("Test", "VehiclePremiumTest");
        waitForTable(editorPage);
        editorPage.getCenterTable().editCell(1, 1, "Test DetermineVehiclePremium VehiclePremiumTest1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilAppIdle();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCountWhenCounted("6"))
                .as("The button should read Test and carry the number of test tables after the copy")
                .isEqualTo("6");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestButtonText())
                .as("The button should read Test and carry the number of test tables after the copy")
                .isEqualTo("Test6");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCount())
                .as("The button should say there are 6 test tables after copying the module")
                .isEqualTo("6");

        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();

        DriverPool.getPage().reload();
        editorPage = new EditorPage();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCountWhenCounted("6"))
                .as("The button should read Test and carry the number of test tables after a refresh")
                .isEqualTo("6");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestButtonText())
                .as("The button should read Test and carry the number of test tables after a refresh")
                .isEqualTo("Test6");
        assertThat(editorPage.getEditorToolbarPanelComponent().getTestCount())
                .as("The button should still say there are 6 test tables after a refresh")
                .isEqualTo("6");

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, NAME_PROJECT_MY, "MyProject.zip");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameExample3Project);
        DriverPool.getPage().goBack();
        editorPage = new EditorPage();
        editorPage.reloadPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(NAME_PROJECT_MY, "module_KS");
        editorPage.getProblemsPanelComponent().waitForCompilationProgressBarToContain("Loaded", 200000);
    }
    private void waitForTable(EditorPage editorPage) {
        WaitUtil.waitForCondition(() -> editorPage.getCenterTable().isVisible(),
                TABLE_LOAD_TIMEOUT_MS, 250, "Waiting for the table of the selected node");
        editorPage.waitUntilSpinnerLoaded();
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.waitUntilAppIdle();
    }
}
