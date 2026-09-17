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
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefaultProperties extends BaseTest {

    private static final String PROJECT_NAME = "TestDefaultProperties";

    @Test
    @TestCaseId("IPBQA-27254")
    @Description("Rules Editor - Check default properties (Name field) displayed in table details")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDefaultProperties() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProject(CreateNewProjectComponent.TabName.EXCEL_FILES,
                PROJECT_NAME, "TestDefaultProperties.xlsx");

        editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(PROJECT_NAME, "TestDefaultProperties");

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Run")
                .selectItemInFolder("Run", "SpreadsheetTable");

        // The name a table goes by is what the panel is headed with, and three tables of this module are
        // called SpreadsheetTable, so the name carries what tells them apart. A Run table declares nothing
        // else, and the panel says so rather than listing anything.
        assertThat(editorPage.getRightTableDetailsComponent().getTitle())
                .as("The panel should be headed with the name of the Run table")
                .startsWith("SpreadsheetTable$Run$");
        assertThat(editorPage.getRightTableDetailsComponent().saysNoProperties())
                .as("The panel should say that the Run table declares no property of its own")
                .isTrue();

        editorPage.getEditorLeftRulesTreeComponent()
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "SpreadsheetTable");

        assertThat(editorPage.getRightTableDetailsComponent().getTitle())
                .as("The panel should be headed with the name of the Test table")
                .startsWith("SpreadsheetTable$Test$");
        assertThat(editorPage.getRightTableDetailsComponent().saysNoProperties())
                .as("The panel should say that the Test table declares no property of its own")
                .isTrue();
    }
}
