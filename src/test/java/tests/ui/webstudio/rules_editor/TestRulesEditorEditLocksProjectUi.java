package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestRulesEditorEditLocksProjectUi extends BaseTest {

    private static final String TUTORIAL_1 = "Tutorial 1 - Introduction to Decision Tables";
    private static final String MODULE = "Tutorial1 - Intro to Decision Tables";
    private static final String TABLE = "DriverPremium1";
    private static final UserData SECOND_EDITOR = new UserData("editor2", "editor2");

    @Test
    @TestCaseId("EPBDS-16879")
    @Description("EPBDS-16724: pressing Edit on a table locks the project. Another user with write rights, who is "
            + "offered Edit on the same table before, sees it read-only while the first user edits, and the first "
            + "user's Save succeeds instead of being refused as locked by the other user.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void editLocksTheProjectForAnotherUser() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TUTORIAL_1);
        EditorPage editorPage = new EditorPage();
        addContributor(editorPage);
        selectTable(editorPage, projectName);

        DriverPool.BrowserSession secondEditor = DriverPool.openAnotherSession();
        try {
            DriverPool.actIn(secondEditor, () -> {
                EditorPage otherPage = openTableAsSecondEditor(projectName);
                assertThat(otherPage.getEditorToolbarPanelComponent().getEditTableBtn().isVisible(5000))
                        .as("While nobody edits the table, the second user must be offered Edit")
                        .isTrue();
            });

            TableComponent table = editorPage.getCenterTable();
            int youngMarried = table.getRowIndexHolding("Young Driver", "Married");
            editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
            table.editCell(youngMarried, 3, "777");

            DriverPool.actIn(secondEditor, () -> {
                EditorPage otherPage = new EditorPage().reloadPage();
                selectTableInTree(otherPage);
                EditorToolbarPanelComponent toolbar = otherPage.getEditorToolbarPanelComponent();
                assertThat(otherPage.getCenterTable().getCellTexts())
                        .as("The second user sees the table as it is saved")
                        .contains("Young Driver", "700");
                assertThat(toolbar.isRunButtonVisible())
                        .as("The toolbar of the table is drawn for the second user")
                        .isTrue();
                assertThat(toolbar.getEditTableBtn().isVisible(5000))
                        .as("While the first user edits the table, the second user must not be offered Edit")
                        .isFalse();
            });
        } finally {
            secondEditor.close();
        }

        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.reloadPage();
        selectTableInTree(editorPage);
        assertThat(editorPage.getCenterTable().getCellTexts())
                .as("The first user's Save must succeed and write the edit")
                .contains("777")
                .doesNotContain("700");
    }

    private EditorPage openTableAsSecondEditor(String projectName) {
        EditorPage otherPage = new LoginService(DriverPool.getPage()).login(SECOND_EDITOR);
        RepositoryPage repositoryPage = otherPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openIfClosed(projectName);
        otherPage = new EditorPage();
        selectTable(otherPage, projectName);
        return otherPage;
    }

    private void addContributor(EditorPage editorPage) {
        UsersPageComponent users = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        users.clickAddUser()
                .setUsername(SECOND_EDITOR.getLogin())
                .setPassword(SECOND_EDITOR.getPassword())
                .saveUser();
        users.clickEditUser(SECOND_EDITOR.getLogin())
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Contributor")
                .saveUser();
    }

    private void selectTable(EditorPage editorPage, String projectName) {
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        selectTableInTree(editorPage);
    }

    private void selectTableInTree(EditorPage editorPage) {
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, TABLE);
    }
}
