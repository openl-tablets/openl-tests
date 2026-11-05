package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent;
import domain.ui.webstudio.components.admincomponents.AuditLogPageComponent.AuditEvent;
import domain.ui.webstudio.components.admincomponents.UsersPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TestACLProjectLevelRoles extends BaseTest {

    @Test
    @TestCaseId({"IPBQA-32912", "EPBDS-17079", "EPBDS-16598"})
    @Description("ACL: project-level role assignment (Manager/Viewer on specific projects) and permission verification without external auth system. "
            + "The audit log records the refused and the accepted sign-ins, the projects created and the roles granted and changed, "
            + "finds the events of one project and exports them (EPBDS-14601). "
            + "The Username field of the Add access dialog on the Management tab of a project hints at a username, not at an email address (EPBDS-16598)")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testACLProjectLevelRoles() {
        LoginService loginService = new LoginService(DriverPool.getPage());

        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage adminRepoPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String project1Name = StringUtil.generateUniqueName("ProjectRoleTest1");
        adminRepoPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, project1Name, "Example 1 - Bank Rating");

        WaitUtil.waitForCondition(
                () -> adminRepoPage.getAllVisibleProjectsInTable().contains(project1Name),
                15000,
                500,
                "Waiting for project '" + project1Name + "' to appear in the repository table."
        );

        String project2Name = StringUtil.generateUniqueName("ProjectRoleTest2");
        adminRepoPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, project2Name, "Sample Project");

        ProjectDetailPage projectCard = adminRepoPage.openProjectsList().openProjectDetail(project1Name);
        String usernameHint = projectCard.openAddAccessDialogAndGetUsernameHint();
        assertThat(usernameHint)
                .as("The Username field of Add access hints at a username, not at an email address (EPBDS-16598)")
                .isEqualTo("username")
                .doesNotContain("@");
        projectCard.cancelAddAccessDialog();
        adminRepoPage.openProjectsList();

        UsersPageComponent usersComponent = editorPage
                .openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();

        assertThat(usersComponent.isUserInList("admin")).as("Admin user should be in the users list").isTrue();

        int initialUserCount = usersComponent.getUsersCount();
        usersComponent.clickAddUser()
                .setUsername("test")
                .setPassword("test")
                .saveUser();

        assertThat(usersComponent.isUserInList("test")).as("User 'test' should be added to the list").isTrue();
        assertThat(usersComponent.getUsersCount()).as("User count should increase by 1").isEqualTo(initialUserCount + 1);

        editorPage.openUserMenu().signOut();
        LoginPage refusedLogin = new LoginPage();
        refusedLogin.login(new UserData("test", "wrong"));
        assertThat(refusedLogin.isLoginErrorDisplayed(10000)).as("A wrong password of test is refused").isTrue();
        UserData testUser = new UserData("test", "test");
        editorPage = loginService.login(testUser);
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        List<String> visibleProjects = repositoryPage.getAllVisibleProjectsInTable();
        assertThat(visibleProjects).isEmpty();
        editorPage.openUserMenu().signOut();

        editorPage = loginService.login(UserService.getUser(User.ADMIN));
        usersComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();

        usersComponent.clickEditUser("test")
                .clickProjectsTab()
                .clickAddRoleBtn()
                .setProject(0, project1Name)
                .setProjectRole(0, "Manager")
                .clickAddRoleBtn()
                .setProject(1, project2Name)
                .setProjectRole(1, "Manager")
                .saveUser();

        assertThat(usersComponent.isUserInList("test")).isTrue();

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(testUser);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        visibleProjects = repositoryPage.getAllVisibleProjectsInTable();
        assertThat(visibleProjects)
            .as("User 'test' should see BOTH projects with assigned Manager roles")
            .contains(project1Name)
            .contains(project2Name);

        List<String> managerActions = repositoryPage.getProjectActionLabels(project1Name);
        assertThat(managerActions)
                .as("Project Manager: Export visible, NOT Delete/Deploy (project-scoped). Actual: %s", managerActions)
                .contains("Export")
                .doesNotContain("Delete", "Deploy");

        if (repositoryPage.isProjectActionAvailable(project1Name, "Open")) {
            repositoryPage.openProject(project1Name);
        }
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(project1Name, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree("Rating Algorithm")
                .selectItemInFolder("Rating Algorithm", "BankRatingCalculation");
        EditorToolbarPanelComponent managerToolbar = editorPage.getEditorToolbarPanelComponent();
        assertThat(managerToolbar.getEditTableBtn().isVisible(2000))
                .as("Project Manager should see Edit button for tables in Editor")
                .isTrue();

        editorPage.openUserMenu().signOut();

        editorPage = loginService.login(UserService.getUser(User.ADMIN));

        usersComponent = editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage();

        usersComponent.clickEditUser("test")
                .clickProjectsTab()
                .setProjectRole(0, "Viewer")
                .setProjectRole(1, "Viewer")
                .saveUser();

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(testUser);
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        visibleProjects = repositoryPage.getAllVisibleProjectsInTable();
        assertThat(visibleProjects)
            .as("User 'test' should still see BOTH projects with Viewer roles")
            .contains(project1Name)
            .contains(project2Name);

        List<String> viewerActions = repositoryPage.getProjectActionLabels(project1Name);
        assertThat(viewerActions)
                .as("Viewer: Export visible, NOT Copy/Delete/Deploy/Save. Actual: %s", viewerActions)
                .contains("Export")
                .doesNotContain("Copy", "Delete", "Deploy", "Save");

        if (repositoryPage.isProjectActionAvailable(project1Name, "Open")) {
            repositoryPage.openProject(project1Name);
        }
        editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(project1Name, "Bank Rating");

        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree("Rating Algorithm")
                .selectItemInFolder("Rating Algorithm", "BankRatingCalculation");

        EditorToolbarPanelComponent toolbarPanel = editorPage.getEditorToolbarPanelComponent();
        assertThat(toolbarPanel.getEditTableBtn().isVisible(2000))
                .as("Viewer should NOT see Edit button for tables in Editor")
                .isFalse();

        editorPage.openUserMenu().signOut();
        editorPage = loginService.login(UserService.getUser(User.ADMIN));
        AuditLogPageComponent auditLog = editorPage.openUserMenu().navigateToAdministration().navigateToAuditLogPage();
        List<AuditEvent> events = auditLog.getEvents();
        AuditEvent refused = events.stream()
                .filter(event -> event.user().equals("test") && event.action().equals("Sign in") && event.outcome().equals("Failure"))
                .findFirst().orElseThrow(() -> new AssertionError("No refused sign-in of test in " + events));
        assertThat(auditLog.getDetails(refused)).as("The refused sign-in names the reason").contains("Wrong credentials");
        assertThat(events).as("The sign-in of admin through the login form is recorded: %s", events)
                .anyMatch(event -> signedInWithLoginForm(event, "admin"));
        assertThat(events).as("The sign-in of test through the login form is recorded: %s", events)
                .anyMatch(event -> signedInWithLoginForm(event, "test"));
        for (String project : List.of(project1Name, project2Name)) {
            assertThat(events).as("The creation of %s is recorded: %s", project, events)
                    .anyMatch(event -> event.action().equals("Project created") && event.object().contains(project));
            assertThat(roleDetails(auditLog, events, "Role granted", project))
                    .as("admin granting test the Manager role on %s is recorded", project)
                    .anyMatch(details -> details.contains("Manager"));
            assertThat(roleDetails(auditLog, events, "Role changed", project))
                    .as("admin changing the role of test on %s to Viewer is recorded", project)
                    .anyMatch(details -> details.contains("Viewer"));
        }

        List<AuditEvent> ofProject1 = auditLog.filterByObject(project1Name)
                .waitForEvents(shown -> shown.stream().allMatch(event -> event.object().contains(project1Name)));
        assertThat(ofProject1).as("The search by object leaves only the events of %s", project1Name)
                .extracting(AuditEvent::action)
                .contains("Project created", "Role granted", "Role changed");
        List<AuditEvent> all = auditLog.clearFilters()
                .waitForEvents(shown -> shown.stream().anyMatch(event -> event.object().contains(project2Name)));
        assertThat(all).as("Clear filters brings back the events of every object")
                .anyMatch(event -> event.action().equals("Sign in"));
        List<String> exportedIds = auditLog.exportCsv().lines().skip(1)
                .filter(line -> line.contains(",")).map(line -> line.substring(0, line.indexOf(','))).toList();
        assertThat(exportedIds).as("The exported CSV holds every event the log shows")
                .containsAll(all.stream().map(AuditEvent::id).toList());
        AuditLogPageComponent reread = editorPage.openUserMenu().navigateToAdministration().navigateToAuditLogPage();
        assertThat(reread.getEvents().stream().filter(event -> event.action().equals("Audit log read")).map(reread::getDetails))
                .as("The export of the audit log is recorded with its format")
                .anyMatch(details -> details.contains("CSV"));
    }

    private static boolean signedInWithLoginForm(AuditEvent event, String user) {
        return event.user().equals(user) && event.action().equals("Sign in") && event.outcome().equals("Success")
                && event.signedInWith().equals("Login form");
    }

    private static List<String> roleDetails(AuditLogPageComponent auditLog, List<AuditEvent> events, String action, String project) {
        return events.stream()
                .filter(event -> event.user().equals("admin") && event.action().equals(action)
                        && event.object().contains(project) && event.subject().contains("test"))
                .map(auditLog::getDetails)
                .toList();
    }
}
