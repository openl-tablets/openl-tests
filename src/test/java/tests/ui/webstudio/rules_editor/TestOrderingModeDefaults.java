package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.models.UserData;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.LoginPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent.FilterOptions.BY_CATEGORY;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent.FilterOptions.BY_CATEGORY_DETAILED;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent.FilterOptions.BY_CATEGORY_INVERSED;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestOrderingModeDefaults extends BaseTest {

    private static final String PROJECT_ZIP = "TestOrderingMode.zip";
    private static final String MODULE = "DefaultModeTesting";
    private static final String DEFAULT_VIEW = "Excel Sheet";

    @Test
    @TestCaseId("IPBQA-32117")
    @Description("In single-user mode the tables tree opens on the Excel Sheet view until a view is picked, "
            + "and the browser keeps the view picked last when the module is opened again and in a new session.")
    @AppContainerConfig(startParams = AppContainerStartParameters.SINGLE_USER_STUDIO_PARAMS)
    public void testDefaultOrderForSingleUser() {
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        new LoginPage().completeProfileIfRequested();
        createProject(new EditorPage(), projectName);

        EditorLeftRulesTreeComponent rulesTree = openModule(projectName);
        assertThat(rulesTree.getViewFilterValue())
                .as("A browser that has not picked a view should open the tables tree on %s", DEFAULT_VIEW)
                .isEqualTo(DEFAULT_VIEW);
        assertThat(rulesTree.getCategoriesVisible()).contains("Model", "Algorithm", "Test");

        rulesTree.setViewFilter(BY_CATEGORY_INVERSED);
        rulesTree = openModule(projectName);
        assertThat(rulesTree.getViewFilterValue())
                .as("The module opened again should show the view picked last")
                .isEqualTo("Category Inversed");
        assertThat(rulesTree.getCategoriesVisible()).isNotEmpty();

        rulesTree.setViewFilter(BY_CATEGORY);
        assertThat(rulesTree.getCategoriesVisible()).contains("Calculaiton-Spreadsheet", "Calculation-Smart");

        DriverPool.getPage().context().clearCookies();
        DriverPool.getPage().navigate(DriverPool.getAppUrl());
        new LoginPage().completeProfileIfRequested();
        assertThat(openModule(projectName).getViewFilterValue())
                .as("A new session in the same browser should show the view picked last")
                .isEqualTo("Category");
    }

    @Test
    @TestCaseId("IPBQA-32117")
    @Description("In multi-user mode the tables tree opens on the Excel Sheet view until a view is picked, the browser "
            + "keeps the view picked last for whoever signs in next, and the Type view groups the tables by their kind, "
            + "an alias datatype standing in a Vocabulary group of its own.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDefaultOrderForMultiUser() {
        String projectName = StringUtil.generateUniqueName("TestOrderingMode");
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(new UserData("admin", "admin"));
        createProject(editorPage, projectName);

        EditorLeftRulesTreeComponent rulesTree = openModule(projectName);
        assertThat(rulesTree.getViewFilterValue())
                .as("A browser that has not picked a view should open the tables tree on %s", DEFAULT_VIEW)
                .isEqualTo(DEFAULT_VIEW);

        rulesTree.setViewFilter(BY_CATEGORY_DETAILED);
        rulesTree = openModule(projectName);
        assertThat(rulesTree.getViewFilterValue())
                .as("The module opened again should show the view picked last")
                .isEqualTo("Category Detailed");
        assertThat(rulesTree.getCategoriesVisible()).isNotEmpty();

        editorPage = new EditorPage();
        editorPage.openUserMenu()
                .navigateToAdministration()
                .navigateToUsersPage()
                .clickAddUser()
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
        editorPage = new LoginService(DriverPool.getPage()).login(new UserData("user1", "user1"));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        if (repositoryPage.isProjectActionAvailable(projectName, "Open")) {
            repositoryPage.openProject(projectName);
        }
        rulesTree = openModule(projectName);
        assertThat(rulesTree.getViewFilterValue())
                .as("The browser should show the view picked last to whoever signs in next")
                .isEqualTo("Category Detailed");

        rulesTree.setViewFilter(BY_TYPE);
        assertThat(rulesTree.getFoldersVisible())
                .as("The Type view should group the tables by their kind, Vocabulary among them")
                .contains(DECISION, "Spreadsheet", "Test", "Datatype", "Vocabulary");
    }

    private void createProject(EditorPage editorPage, String projectName) {
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE, projectName, PROJECT_ZIP);
    }

    private EditorLeftRulesTreeComponent openModule(String projectName) {
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE);
        return editorPage.getEditorLeftRulesTreeComponent();
    }
}
