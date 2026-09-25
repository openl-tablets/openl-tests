package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.ZipUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDeleteOpenApiFileRemovesProperties extends BaseTest {

    private static final String YML_FILE = "openapi.yml";

    @Test
    @TestCaseId("IPBQA-30678")
    @Description("Delete OpenAPI file from repository and verify OpenAPI properties section becomes empty in Editor")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testDeleteOpenApiFileRemovesProperties() {
        String projectName = "YmlOpenApiProject_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProjectFromOpenApi(YML_FILE, projectName);

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        assertThat(editorPage.isOpenApiDeclaredByDefault())
                .as("The card should name the specification the project holds without declaring it")
                .isTrue();
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("The specification is kept under the name its format reads as")
                .isEqualTo("openapi.yaml");

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.openProjectsList().openProjectDetail(projectName).deleteFile("openapi.yaml");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        editorPage.reloadPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        assertThat(editorPage.isOpenApiPropertiesSectionEmpty())
                .as("OpenAPI properties section should be empty after deleting the OpenAPI file")
                .isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Algorithms");
        editorPage.getProblemsPanelComponent().checkNoProblems();

        editorPage.getEditorToolbarPanelComponent().navigateToProjectRoot(projectName);
        editorPage.getEditorToolbarPanelComponent().clickExport();
        File exportedZip = editorPage.getExportProjectDialogComponent().clickExportAndDownload();
        assertThat(ZipUtil.listFiles(exportedZip))
                .as("The workbooks of the two modules should still stand after deleting openapi.yml")
                .contains("rules/Algorithms.xlsx", "rules/Models.xlsx");
        String rulesXml = ZipUtil.readFileFromZip(exportedZip, "rules.xml");
        assertThat(rulesXml)
                .as("The descriptor should declare no OpenAPI specification, before the deletion or after it")
                .doesNotContain("<openapi>");
    }
}
