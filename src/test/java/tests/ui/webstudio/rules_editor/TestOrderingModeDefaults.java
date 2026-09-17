package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
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

public class TestOrderingModeDefaults extends BaseTest {

    @Test
    @TestCaseId("IPBQA-32117")
    @AppContainerConfig(startParams = AppContainerStartParameters.SINGLE_USER_STUDIO_PARAMS)
    public void testDefaultOrderForSingleUser() {
        // 1.1 Start Webstudio in single user mode — handled by @AppContainerConfig
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        // Single-user mode signs in without a login form, so the "Complete Your Profile" modal has to be
        // dealt with here or it blocks the page.
        new LoginPage().completeProfileIfRequested();
        EditorPage editorPage = new EditorPage();

        // 1.2 Verification of the default value of "Default Order:"
        AdminPage adminPage = editorPage.openUserMenu().navigateToMySettings();
        MySettingsPageComponent mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        // 1.4 Verification that the default "Default Order:" value is applied
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

        // 1.5 Verification table nodes
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible())
                .contains("Model", "Algorithm", "Test");

        // 1.6 Modify the "Default Order:" value
        adminPage = editorPage.openUserMenu().navigateToMySettings();
        mySettings = adminPage.navigateToMySettingsPage();
        mySettings.setDefaultOrder("By Category Inversed");
        mySettings.saveSettings();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Category Inversed");

        // 1.7 What the inversed view files the tables under: see KNOWN-ISSUES.md #14.
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible()).isNotEmpty();

        // 1.8 Verification that is not overridden by another mode choosing
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_CATEGORY);

        // 1.9 The sheets of the tables that declare no category are no longer listed: KNOWN-ISSUES.md #14.
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible())
                .contains("Calculaiton-Spreadsheet", "Calculation-Smart");

        // 1.10 Whether the Default Order still applies once a view has been chosen by hand and the browser
        // reopened: see KNOWN-ISSUES.md #14.
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
            + "the tables into. Fails on EPBDS-16654: an alias datatype is filed under Datatype, the tree not "
            + "reading the Vocabulary kind the server marks it with.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16654")
    public void testDefaultOrderForMultiUser() {
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(new UserData("admin", "admin"));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(
                domain.ui.webstudio.components.common.CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                projectName, "TestOrderingMode.zip");

        // 2.2 Verification of the default value of "Default Order:" for admin
        AdminPage adminPage = editorPage.openUserMenu().navigateToMySettings();
        MySettingsPageComponent mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        // 2.3 Change default order for admin to "By Category Detailed"
        mySettings.setDefaultOrder("By Category Detailed");
        mySettings.saveSettings();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Category Detailed");

        // 2.4 What the detailed view files the tables under: see KNOWN-ISSUES.md #14.
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getCategoriesVisible()).isNotEmpty();

        // 2.5 Verification that the default "Default Order:" is different for another user.
        // The legacy Keycloak setup pre-provisioned an openl_1 account; without that
        // infrastructure we now create a regular user via the admin UI and reuse it here.
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

        // Check default order for user1 — should still be "By Excel Sheet" (default)
        adminPage = editorPage.openUserMenu().navigateToMySettings();
        mySettings = adminPage.navigateToMySettingsPage();
        assertThat(mySettings.getDefaultOrder()).isEqualTo("By Excel Sheet");

        // 2.6 Change default order for user1 to "By Type"
        mySettings.setDefaultOrder("By Type");
        mySettings.saveSettings();

        editorPage = new EditorPage();
        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        // The project belongs to the other user's workspace, so this user has to open it before the editor
        // tree shows it. (Locking is gone from the React UI, so unlockAllProjects has nothing to do.)
        if (repositoryPage.isProjectActionAvailable(projectName, "Open")) {
            repositoryPage.openProject(projectName);
        }
        new EditorPage();
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "DefaultModeTesting");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getViewFilterValue())
                .containsIgnoringCase("Type");

        // 2.7 The Type view groups the tables by the kind each one is, and an alias datatype is a
        // Vocabulary — the server marks it as one in the very answer the tree is drawn from (EPBDS-16654).
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getFoldersVisible())
                .as("The Type view should group the tables by their kind, Vocabulary among them")
                .contains("Rules", "Spreadsheet", "Test", "Datatype", "Vocabulary");
    }
}
