package tests.ui.webstudio.studio_issues;

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
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestConstructorNotFoundInDependentTable extends BaseTest {

    @Test
    @TestCaseId("EPBDS-12848")
    @Description("EPBDS-12848: 'Constructor not found' error must not appear in dependent table after modifying main table")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testNoConstructorNotFoundErrorInDependentTable() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, "DoliMy", "EPBDS-12848_DoliMy.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, "TheProejct", "EPBDS-12848_TheProejct.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, "SomeTransProject", "EPBDS-12848_SomeTransProject.zip");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule("DoliMy", "DoliMy");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Spreadsheet")
                .selectItemInFolder("Spreadsheet", "mySpr3");
        assertThat(editorPage.getProblemsPanelComponent().hasErrors())
                .as("DoliMy/mySpr3 should have no errors initially")
                .isFalse();

        editorPage.getEditorToolbarPanelComponent().getBreadcrumbsAllProjects().click();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule("TheProejct", "TheModel");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "someRule");
        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(1, 1, "SimpleRules String someRuleModified()");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.getEditorToolbarPanelComponent().getBreadcrumbsAllProjects().click();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule("DoliMy", "DoliMy");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Spreadsheet")
                .selectItemInFolder("Spreadsheet", "mySpr3");

        assertThat(editorPage.getProblemsPanelComponent().hasErrors())
                .as("EPBDS-12848: DoliMy/mySpr3 must have no errors after modifying TheProejct/someRule")
                .isFalse();
    }
}
