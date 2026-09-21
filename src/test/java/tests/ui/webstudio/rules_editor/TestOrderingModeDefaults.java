package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.MySettingsPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.AdminPage;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestOrderingModeDefaults extends BaseTest {

    @Test
    @TestCaseId("IPBQA-32117")
    @AppContainerConfig(startParams = AppContainerStartParameters.SINGLE_USER_STUDIO_PARAMS)
    public void testDefaultOrderForSingleUser() {
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        new LoginPage().completeProfileIfRequested();
        EditorPage editorPage = new EditorPage();

        AdminPage adminPage = editorPage.openUserMenu().navigateToMySettings();
        MySettingsPageComponent mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        adminPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        RepositoryPage repositoryPage = new RepositoryPage();
        repositoryPage.createProject(
                domain.ui.webstudio.components.common.CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                projectName, "TestOrderingMode.zip");
        new EditorPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Excel Sheet");

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible())
                .contains("Model", "Algorithm", "Test");

        adminPage = editorPage.openUserMenu().navigateToMySettings();
        mySettings = adminPage.navigateToMySettingsPage();
        mySettings.setDefaultOrder("By Category Inversed");
        mySettings.saveSettings();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Category Inversed");

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible()).isNotEmpty();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_CATEGORY);

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible())
                .contains("Calculaiton-Spreadsheet", "Calculation-Smart");

        DriverPool.getPage().context().clearCookies();
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        new LoginPage().completeProfileIfRequested();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue()).isNotEmpty();
    }

    @Test
    @TestCaseId("IPBQA-32117")
    @Description("The default view of the tables tree in multi-user mode, and the groups the Type view puts "
            + "the tables into, an alias datatype standing in a Vocabulary group of its own.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDefaultOrderForMultiUser() {
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(new UserData("admin", "admin"));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(
                domain.ui.webstudio.components.common.CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                projectName, "TestOrderingMode.zip");

        AdminPage adminPage = editorPage.openUserMenu().navigateToMySettings();
        MySettingsPageComponent mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        mySettings.setDefaultOrder("By Category Detailed");
        mySettings.saveSettings();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Category Detailed");

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible()).isNotEmpty();

        UsersPageComponent usersPage = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        usersPage.clickAddUser()
                .setUsername("user1")
                .setEmail("user1@example.com")
                .setFirstName("First Name")
                .setLastName("Last Name")
                .setPassword("user1")
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Manager")
                .saveUser();

        editorPage.openUserMenu().signOut();
        loginService = new LoginService(DriverPool.getPage());
        editorPage = loginService.login(new UserData("user1", "user1"));

        adminPage = editorPage.openUserMenu().navigateToMySettings();
        mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        mySettings.setDefaultOrder("By Type");
        mySettings.saveSettings();

        editorPage = new EditorPage();
        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        if (repositoryPage.isProjectActionAvailable(projectName, "Open")) {
            repositoryPage.openProject(projectName);
        }
        new EditorPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Type");

        assertThat(editorPage.getEditorLeftRulesTreeComponent().getFoldersVisible())
                .as("The Type view should group the tables by their kind, Vocabulary among them")
                .contains(DECISION, "Spreadsheet", "Test", "Datatype", "Vocabulary");
    }
}
