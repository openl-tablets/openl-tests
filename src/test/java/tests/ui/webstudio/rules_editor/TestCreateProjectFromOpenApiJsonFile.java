package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TableComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCreateProjectFromOpenApiJsonFile extends BaseTest {

    private static final String JSON_FILE = "openapi.json";

    @Test
    @TestCaseId("IPBQA-30678")
    @Description("Create project from OpenAPI JSON file and verify repository tree structure and Editor module properties;"
            + " copying a module keeps the modules the descriptor already declares (EPBDS-16227) and an uploaded"
            + " workbook does not rewrite an explicit rules.xml.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCreateProjectFromOpenApiJsonFile() {
        String projectName = "JsonOpenApiProject_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProjectFromOpenApi(JSON_FILE, projectName);

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(projectName).openFilesTab();
        assertThat(projectDetail.isFilePresent("Algorithms.xlsx"))
                .as("Algorithms.xlsx should be present in the project files").isTrue();
        assertThat(projectDetail.isFilePresent("Models.xlsx"))
                .as("Models.xlsx should be present in the project files").isTrue();
        assertThat(projectDetail.isFilePresent("openapi.json"))
                .as("openapi.json should be present in the project files").isTrue();
        assertThat(projectDetail.isFilePresent("rules.xml"))
                .as("rules.xml should be present in the project files").isTrue();
        assertThat(projectDetail.isFilePresent("rules-deploy.xml"))
                .as("rules-deploy.xml should be present in the project files").isTrue();
        repositoryPage.openProjectsList();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        // A project made from a specification is checked against it and not written from it again, so the
        // card names the file it found and the reconciliation it does with it, and names no module to
        // write into until a generation is asked for (EPBDS-16415).
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("OpenAPI File property should reflect uploaded file name").isEqualTo("openapi.json");
        assertThat(editorPage.getOpenApiMode())
                .as("A project created from a specification is reconciled against it").isEqualTo("Reconciliation");
        assertThat(editorPage.hasOpenApiProperty("Services module"))
                .as("No module is named to write the rules into until a generation is asked for").isFalse();
        assertThat(editorPage.hasOpenApiProperty("Data types module"))
                .as("No module is named to write the data types into until a generation is asked for").isFalse();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms");
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet"))
                .as("Algorithms module should have Spreadsheet folder").isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Environment"))
                .as("Algorithms module should have Configuration folder").isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Datatype"))
                .as("Algorithms module should NOT have Datatype folder").isFalse();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Models");
        editorPage.getEditorLeftRulesTreeComponent().setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE);
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Datatype"))
                .as("Models module should have Datatype folder").isTrue();
        assertThat(editorPage.getEditorLeftRulesTreeComponent().isFolderExistsInTree("Spreadsheet"))
                .as("Models module should NOT have Spreadsheet folder").isFalse();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorLeftRulesTreeComponent().expandFolderInTree("Datatype");
        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder("Datatype", "JAXRSErrorResponse");
        TableComponent datatypeTable = editorPage.getCenterTable();
        datatypeTable.editCell(3, 1, "String[]");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.waitUntilSpinnerLoaded();
        editorPage.waitUntilAppIdle();
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.reloadPage();
        editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        editorPage.getEditorToolbarPanelComponent().clickExport();
        File exportedZip = editorPage.getExportProjectDialogComponent().clickExportAndDownload();

        String deployXml = ZipUtil.readFileFromZip(exportedZip, "rules-deploy.xml");
        assertThat(deployXml).as("rules-deploy.xml should contain isProvideRuntimeContext=true")
                .contains("<isProvideRuntimeContext>true</isProvideRuntimeContext>");
        assertThat(deployXml).as("rules-deploy.xml should contain RESTFUL publisher")
                .contains("<publisher>RESTFUL</publisher>");
        assertThat(deployXml).as("rules-deploy.xml should contain annotationTemplateClassName")
                .contains("<annotationTemplateClassName>org.openl.generated.services.Service</annotationTemplateClassName>");

        String rulesXml = ZipUtil.readFileFromZip(exportedZip, "rules.xml");
        // The two modules are written where the engine looks for them anyway, so the descriptor says nothing
        // about them and nothing about the generation either; both are found by the standard layout.
        assertThat(rulesXml).as("The descriptor repeats neither module, which the standard layout finds")
                .doesNotContain("<modules>").doesNotContain("<rules-root");
        assertThat(rulesXml).as("The descriptor holds no generation settings, so later edits are not written over")
                .doesNotContain("<openapi>");
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("Both generated modules are read all the same")
                .containsExactlyInAnyOrder("Algorithms", "Models");

        // A module is named after the workbook it reads where the project finds its modules by the standard
        // layout, so a module is renamed and copied by renaming and copying that workbook on the Files tab.
        editorPage.renameModuleWorkbook(projectName, "Algorithms.xlsx", "Algorithms_test.xlsx");
        editorPage.renameModuleWorkbook(projectName, "Models.xlsx", "Models_test.xlsx");
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("A module renamed with its workbook is read under the new name")
                .containsExactlyInAnyOrder("Algorithms_test", "Models_test");

        editorPage.copyModuleWorkbook(projectName, "Algorithms_test.xlsx", "Algorithms2.xlsx");
        assertThat(editorPage.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("Copying a module must add it next to the modules the project already reads (EPBDS-16227)")
                .containsExactlyInAnyOrder("Algorithms2", "Algorithms_test", "Models_test");

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage detailAfterUpload = repositoryPage.openProjectsList().openProjectDetail(projectName);
        detailAfterUpload.uploadFileInto(TestDataUtil.getFilePathFromResources("rules.xlsx"), "rules");
        assertThat(detailAfterUpload.isFilePresent("rules.xlsx"))
                .as("rules.xlsx should be present in the project files after upload").isTrue();
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded rules.xlsx");

        EditorPage editorPageAfterUpload = new EditorPage();
        editorPageAfterUpload.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        assertThat(editorPageAfterUpload.getEditorLeftProjectModuleSelectorComponent().getAllModuleNames(projectName))
                .as("A workbook put in the rules folder is read as a module of its own, and the ones already "
                        + "there are kept")
                .containsExactlyInAnyOrder("Algorithms2", "Algorithms_test", "Models_test", "rules");

        editorPageAfterUpload.getEditorToolbarPanelComponent().clickExport();
        File exportedZipAfterUpload = editorPageAfterUpload.getExportProjectDialogComponent().clickExportAndDownload();
        String rulesXmlAfterUpload = ZipUtil.readFileFromZip(exportedZipAfterUpload, "rules.xml");
        assertThat(rulesXmlAfterUpload)
                .as("The descriptor is left declaring no module of its own through all of this")
                .doesNotContain("<modules>").doesNotContain("<rules-root");
        assertThat(ZipUtil.listFiles(exportedZipAfterUpload))
                .as("The saved project holds the workbooks as they were renamed, copied and uploaded")
                .contains("rules/Algorithms_test.xlsx", "rules/Algorithms2.xlsx", "rules/Models_test.xlsx",
                        "rules/rules.xlsx");

        repositoryPage = editorPageAfterUpload.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().copyProject(projectName, projectName + "-Copy");
        assertThat(repositoryPage.isProjectPresent(projectName + "-Copy"))
                .as("Copied project '" + projectName + "-Copy' should appear in the projects list").isTrue();
    }
}
