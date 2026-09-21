package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.ui.webstudio.components.common.TableComponent;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.admincomponents.MyProfilePageComponent;
import domain.ui.webstudio.components.admincomponents.MySettingsPageComponent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.serviceclasses.constants.User.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import domain.ui.webstudio.components.editortabcomponents.toolbar.IRunTestsMenu;
import domain.ui.webstudio.components.editortabcomponents.toolbar.ITraceWindow;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestAdminUserSettings extends BaseTest {

    @Test
    @TestCaseId("IPBQA-31293")
    @Description("User settings and profile management")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testUserSettingsAndDetails() {
        LoginService loginService = new LoginService(DriverPool.getPage());

        EditorPage editorPage = loginService.login(UserService.getUser(ADMIN));
        MyProfilePageComponent myProfileComponent = editorPage
                .openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();

        myProfileComponent.setFirstName("")
                .setLastName("")
                .setEmail("")
                .setDisplayName("");
        assertThat(myProfileComponent.getValidationErrors().stream()
                        .anyMatch(message -> message.contains("Email is required"))).as("Clearing the required profile fields must be reported on the form").isTrue();

        myProfileComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();

        assertThat(myProfileComponent.getUsername()).as("Username should be admin").isEqualTo("admin");
        assertThat(myProfileComponent.getEmail().isBlank()).as("Email should keep its stored value").isFalse();
        assertThat(myProfileComponent.getDisplayName().isBlank()).as("Display name should keep its stored value").isFalse();

        myProfileComponent
                .setFirstName("Abc")
                .setLastName("Bcd")
                .setEmail("admin@admin.com")
                .setDisplayNamePattern("First Last")
                .saveProfile();

        myProfileComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();

        assertThat(myProfileComponent.getFirstName()).as("First name should be 'Abc'").isEqualTo("Abc");
        assertThat(myProfileComponent.getLastName()).as("Last name should be 'Bcd'").isEqualTo("Bcd");
        assertThat(myProfileComponent.getEmail()).as("Email should be 'admin@admin.com'").isEqualTo("admin@admin.com");
        assertThat(myProfileComponent.getDisplayName()).as("Display name should be 'Abc Bcd'").isEqualTo("Abc Bcd");

        UsersPageComponent usersComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();

        int adminRow = usersComponent.getUserRow("admin");
        String adminFullName = usersComponent.getFullNameFromRow(adminRow);
        String[] nameParts = adminFullName.split(" ");

        assertThat(nameParts[0]).as("Admin first name in Users table should be 'Abc'").isEqualTo("Abc");
        assertThat(nameParts[1]).as("Admin last name in Users table should be 'Bcd'").isEqualTo("Bcd");
        assertThat(usersComponent.getEmailFromRow(adminRow)).as("Admin email in Users table should be 'admin@admin.com'").isEqualTo("admin@admin.com");
        assertThat(adminFullName).as("Admin display name in Users table should be 'Abc Bcd'").isEqualTo("Abc Bcd");

        myProfileComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();

        myProfileComponent.setCurrentPassword("admin").setNewPassword("12345").setConfirmPassword("12345").saveProfile();

        editorPage.openUserMenu().signOut();

        LoginPage loginPage = new LoginPage();
        loginPage.login(new UserData("admin", "admin"));
        assertThat(loginPage.isLoginFormDisplayed(10000))
                .as("The old password must not sign the user in after the password was changed")
                .isTrue();
        assertThat(loginPage.isLoginErrorDisplayed(10000))
                .as("The login form must say that the old password was refused")
                .isTrue();
        assertThat(loginPage.getLoginErrorMessage())
                .as("The refusal must name the credentials rather than fail silently")
                .containsIgnoringCase("credentials");

        UserData newUserData = new UserData("admin", "12345");
        editorPage = loginService.login(newUserData);
        myProfileComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();

        usersComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();

        usersComponent.clickAddUser()
                .setUsername("user1")
                .setEmail("user1@example.com")
                .setFirstName("Aaa")
                .setLastName("Bbb")
                .setPassword("user1")
                .clickAddRoleBtn()
                .setRoleRepository(0, "Design")
                .setRole(0, "Manager")
                .saveUser();
        
        editorPage.openUserMenu().signOut();
        UserData user1Data = new UserData("user1", "user1");
        editorPage = loginService.login(user1Data);
        
        myProfileComponent = editorPage.openUserMenu()
                .navigateToMyProfile()
                .navigateToMyProfilePage();
                
        assertThat(myProfileComponent.getUsername()).as("Username should be 'user1'").isEqualTo("user1");
        assertThat(myProfileComponent.getFirstName()).as("First name should be 'Aaa'").isEqualTo("Aaa");
        assertThat(myProfileComponent.getLastName()).as("Last name should be 'Bbb'").isEqualTo("Bbb");
        assertThat(myProfileComponent.getEmail()).as("Email should be user1@example.com").isEqualTo("user1@example.com");
        
        myProfileComponent.setDisplayName("Bbb Aaa").saveProfile();
        
        myProfileComponent = editorPage.openUserMenu()
                .navigateToMySettings()
                .navigateToMyProfilePage();
        assertThat(myProfileComponent.getDisplayName()).as("Display name should be updated").isEqualTo("Bbb Aaa");

        editorPage.openUserMenu().signOut();
        UserData adminNewPassword = new UserData("admin", "12345");
        editorPage = loginService.login(adminNewPassword);

        usersComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();
        int user1Row = usersComponent.getUserRow("user1");
        assertThat(usersComponent.getFullNameFromRow(user1Row)).as("Display name should be updated in users table").isEqualTo("Bbb Aaa");

        MySettingsPageComponent mySettingsComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMySettingsPage();

        assertThat(mySettingsComponent.isShowHeaderEnabled()).as("Show Header should be true").isTrue();
        assertThat(mySettingsComponent.isShowFormulasEnabled()).as("Show Formulas should be false").isFalse();
        assertThat(mySettingsComponent.getTestsPerPage()).as("Tests per page should be 5").isEqualTo(5);
        assertThat(mySettingsComponent.isFailuresOnlyEnabled()).as("Failures Only should be false").isFalse();
        assertThat(mySettingsComponent.isCompoundResultEnabled()).as("Compound Result should be false").isFalse();
        assertThat(mySettingsComponent.isShowNumbersWithoutFormattingEnabled()).as("Show numbers without formatting should be false").isFalse();

        myProfileComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMyProfilePage();
        myProfileComponent.setCurrentPassword("12345").setNewPassword("admin").setConfirmPassword("admin").saveProfile();
        editorPage.openUserMenu().signOut();

        String projectNameTest1 = WorkflowService.loginCreateProjectFromExcelFile(ADMIN, "Test1.xlsx");
        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTest1, "Test1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalAdequacyScore");

        TableComponent tableComponent = editorPage.getCenterTable();
        assertThat(tableComponent.getCellText(3, 2)).as("Cell content should be '2500'").isEqualTo("2500");

        mySettingsComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMySettingsPage();
        mySettingsComponent.setShowFormulas(true).setShowHeader(false).saveSettings();

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTest1, "Test1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalAdequacyScore");
        assertThat(tableComponent.getRowsCount()).as("Table should have 7 rows").isEqualTo(7);
        assertThat(tableComponent.getCellText(2, 2)).as("Formula should be visible").isEqualTo("=50*45/D8");

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(user1Data);

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        if (repositoryPage.isProjectActionAvailable(projectNameTest1, "Open")) {
            repositoryPage.openProject(projectNameTest1);
        }
        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTest1, "Test1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalAdequacyScore");

        TableComponent tableComponentUser1 = editorPage.getCenterTable();
        assertThat(tableComponentUser1.getRowsCount()).as("Table should have 8 rows for user1 (different settings)").isEqualTo(8);
        assertThat(tableComponentUser1.getCellText(1, 1)).as("User1 should see different header format").isEqualTo("SimpleRules Double CapitalAdequacyScore (Double capitalAdequacy)");

        editorPage.openUserMenu().signOut();
        String projectNameTemplate = WorkflowService.loginCreateProjectFromTemplate(ADMIN, "Example 1 - Bank Rating");
        mySettingsComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMySettingsPage();

        mySettingsComponent.setTestsPerPage(20)
                .setFailuresOnly(true)
                .setCompoundResult(true)
                .saveSettings();

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTemplate, "Bank Rating");

        IRunTestsMenu testSettings = editorPage.getEditorToolbarPanelComponent().clickTestDropdown();
        assertThat(testSettings.getTestPerPage()).as("Tests per page should be 20").isEqualTo("20");
        assertThat(testSettings.isFailuresOnlyChecked()).as("Failures Only should be enabled").isTrue();
        assertThat(testSettings.isCompoundResultChecked()).as("Compound Result should be enabled").isTrue();

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(user1Data);
        mySettingsComponent = editorPage.openUserMenu()
                .navigateToMySettings()
                .navigateToMySettingsPage();

        assertThat(mySettingsComponent.isShowHeaderEnabled()).as("User1 Show Header should still be true").isTrue();
        assertThat(mySettingsComponent.isShowFormulasEnabled()).as("User1 Show Formulas should still be false").isFalse();
        assertThat(mySettingsComponent.getTestsPerPage()).as("User1 Tests per page should still be 5").isEqualTo(5);
        assertThat(mySettingsComponent.isFailuresOnlyEnabled()).as("User1 Failures Only should still be false").isFalse();
        assertThat(mySettingsComponent.isCompoundResultEnabled()).as("User1 Compound Result should still be false").isFalse();

        editorPage.openUserMenu().openHelp();
        String helpUrl = editorPage.getPage().url();
        assertThat(helpUrl.contains("help")).as("Help should open OpenL Tablets documentation").isTrue();

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(UserService.getUser(User.ADMIN));

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTest1, "Test1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Spreadsheet")
                .selectItemInFolder("Spreadsheet", "TotalAssets4");

        ITraceWindow traceWindow =
                editorPage.getEditorToolbarPanelComponent().clickTraceExpectTraceWindow();
        assertThat(traceWindow.getCallTreeTitles())
                .as("trace opens and shows the TotalAssets4 frame")
                .anyMatch(title -> title.contains("TotalAssets4"));
        assertThat(traceWindow.getTracedTableText())
                .as("traced table is rendered for TotalAssets4")
                .contains("TotalAssets4");
        traceWindow.close();

        mySettingsComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMySettingsPage();
        mySettingsComponent.setShowNumbersWithoutFormatting(true).saveSettings();

        new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectNameTest1, "Test1");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Spreadsheet")
                .selectItemInFolder("Spreadsheet", "TotalAssets4");

        traceWindow = editorPage.getEditorToolbarPanelComponent().clickTraceExpectTraceWindow();
        assertThat(traceWindow.getTracedTableText())
                .as("traced table is rendered after enabling showNumbersWithoutFormatting")
                .contains("TotalAssets4");
        traceWindow.close();

        mySettingsComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToMySettingsPage();
        mySettingsComponent.setShowNumbersWithoutFormatting(false).saveSettings();
    }
}