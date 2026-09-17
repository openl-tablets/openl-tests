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
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCreateProjectFromOpenApiYamlWithCustomModuleNames extends BaseTest {

    private static final String YAML_FILE = "new_openapi_1.yaml";

    @Test
    @TestCaseId("IPBQA-30678")
    @Description("Create project from YAML file with custom module names and paths, verify structure and that the "
            + "project saves after a module is removed (regression guard for EPBDS-16361).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateProjectFromOpenApiYamlWithCustomModuleNames() {
        String projectName = "YamlOpenApiProject_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.getCreateProjectLink().click();
        CreateNewProjectComponent openApiComponent = repositoryPage.getCreateNewProjectComponent();
        openApiComponent.selectMethod(CreateNewProjectComponent.TabName.OPEN_API);

        openApiComponent.uploadOpenApiSpec(YAML_FILE);
        openApiComponent.setProjectName(projectName);

        assertThat(openApiComponent.isCreateEnabled())
                .as("Create button should be enabled after uploading file and setting project name").isTrue();

        openApiComponent.setDataModuleName("Data_Types");
        openApiComponent.setDataModulePath("rules/Data_Types.xlsx");
        assertThat(openApiComponent.getDataModulePath())
                .as("Data module path should hold what was typed").isEqualTo("rules/Data_Types.xlsx");

        openApiComponent.setRulesModuleName("Spreadsheets");
        openApiComponent.setRulesModulePath("rules/Spreadsheets.xlsx");
        assertThat(openApiComponent.getRulesModulePath())
                .as("Rules module path should hold what was typed").isEqualTo("rules/Spreadsheets.xlsx");
        openApiComponent.setDataModulePath("rules1/Data_Types_file.xlsx");
        assertThat(openApiComponent.getDataModulePathInputValue())
                .as("Data module path input should reflect custom path").isEqualTo("rules1/Data_Types_file.xlsx");
        openApiComponent.setRulesModulePath("rules/Spreadsheets_file.xlsx");
        openApiComponent.setRulesModulePath("rules/Spreadsheets.xlsx");
        assertThat(openApiComponent.getRulesModulePath())
                .as("Rules module path should read back what was typed").isEqualTo("rules/Spreadsheets.xlsx");

        openApiComponent.clickCreate();
        repositoryPage.fillCommitInfo();
        repositoryPage.waitUntilSpinnerLoaded();

        ProjectDetailPage projectFiles = repositoryPage.openProjectsList().openProjectDetail(projectName);
        assertThat(projectFiles.isFilePresent("Data_Types_file.xlsx"))
                .as("Data_Types_file.xlsx should be present in the project files").isTrue();
        assertThat(projectFiles.isFilePresent("Spreadsheets.xlsx"))
                .as("Spreadsheets.xlsx should be present in the project files").isTrue();
        // The uploaded specification is kept in the project root under the name its format reads as, whatever
        // it was called when it was uploaded (EPBDS-16415).
        assertThat(projectFiles.isFilePresent("openapi.yaml"))
                .as("The uploaded specification should be kept as openapi.yaml").isTrue();
        assertThat(projectFiles.isFilePresent(YAML_FILE))
                .as("The name it was uploaded under is not kept").isFalse();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        assertThat(editorPage.getOpenApiPropertyValue("File")).isEqualTo("openapi.yaml");
        assertThat(editorPage.getOpenApiMode())
                .as("A project created from a specification is reconciled against it").isEqualTo("Reconciliation");
        assertThat(editorPage.hasOpenApiProperty("Services module"))
                .as("No module is named to write the rules into until a generation is asked for").isFalse();
        assertThat(editorPage.hasOpenApiProperty("Data types module"))
                .as("No module is named to write the data types into until a generation is asked for").isFalse();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Spreadsheets");
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet")).isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Datatype")).isFalse();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Data_Types");
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Datatype")).isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet")).isFalse();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);

        // The descriptor names only the module whose workbook lies outside the standard layout; the other is
        // found by the pattern over rules/, so each is renamed where it is named: one on the card, the other
        // by its workbook on the Files tab.
        RepositoryPage repository = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repository.openProjectsList().openProjectDetail(projectName);
        card.getOverviewTab().renameDeclaredModule("Data_Types", "Data_Type_test");
        editorPage = new EditorPage();
        editorPage.renameModuleWorkbook(projectName, "Spreadsheets.xlsx", "Spreadsheets_test.xlsx");

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("Each module is read under the name it was renamed to")
                .containsExactlyInAnyOrder("Spreadsheets_test", "Data_Type_test");

        editorPage.getEditorToolbarPanelComponent().clickExport();
        File exportedZipAfterRename = editorPage.getExportProjectDialogComponent().clickExportAndDownload();
        String rulesXmlAfterRename = ZipUtil.readFileFromZip(exportedZipAfterRename, "rules.xml");
        assertThat(rulesXmlAfterRename)
                .as("The descriptor names the module it declares as it was renamed, and leaves its workbook where it is")
                .contains("<name>Data_Type_test</name>")
                .contains("<rules-root path=\"rules1/Data_Types_file.xlsx\"/>");
        assertThat(ZipUtil.listFiles(exportedZipAfterRename))
                .as("The module found by the pattern is renamed with its workbook")
                .contains("rules/Spreadsheets_test.xlsx")
                .doesNotContain("rules/Spreadsheets.xlsx");

        // Taking a declaration out of the descriptor leaves the workbook where it is, which is what the
        // Files tab is for; the module is no longer read because nothing else leads to that workbook.
        repository = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        card = repository.openProjectsList().openProjectDetail(projectName);
        card.getOverviewTab().removeDeclaredModule("Data_Type_test");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("A module whose declaration was taken out is no longer read")
                .doesNotContain("Data_Type_test");

        editorPage.getEditorToolbarPanelComponent().clickExport();
        File exportedZipAfterDelete = editorPage.getExportProjectDialogComponent().clickExportAndDownload();
        String rulesXmlAfterDelete = ZipUtil.readFileFromZip(exportedZipAfterDelete, "rules.xml");
        assertThat(rulesXmlAfterDelete)
                .as("The descriptor names the removed module no more")
                .doesNotContain("<name>Data_Type_test</name>");
        assertThat(ZipUtil.listFiles(exportedZipAfterDelete))
                .as("The workbook of the removed module is left where it stands")
                .contains("rules1/Data_Types_file.xlsx");
    }
}
