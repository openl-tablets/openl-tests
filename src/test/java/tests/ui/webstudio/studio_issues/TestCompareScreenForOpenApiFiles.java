package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.TestDataUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestCompareScreenForOpenApiFiles extends BaseTest {

    private static final String OPENAPI_FILE_1 = "openapi-compare.json";
    private static final String OPENAPI_FILE_2 = "openapi-compare2.json";
    private static final String OPENAPI_FILE_3 = "openapi-compare3.json";
    private static final String OPENAPI_FILE_NAME = "openapi.json";

    @Test
    @TestCaseId("EPBDS-10548")
    @Description("On conflict resolution screen for OpenAPI file, Compare screen must show 'DESIGN/rules/{projectName}/openapi.json' as file path")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCompareScreenForOpenApiFiles() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN,
                "StudioIssues.TestCompareScreenForOpenApiFiles.zip");

        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        projectDetail.uploadFileAs(TestDataUtil.getFilePathFromResources(OPENAPI_FILE_1), OPENAPI_FILE_NAME);
        repositoryPage.openProjectsList().saveProject(projectName, "Uploaded " + OPENAPI_FILE_NAME);


        projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        projectDetail.pickUpdateFile(OPENAPI_FILE_NAME, TestDataUtil.getFilePathFromResources(OPENAPI_FILE_2));
        assertThat(projectDetail.isUpdateFileNameWarningShown())
                .as("Warning should appear when uploading a file with a different name")
                .isTrue();
        projectDetail.confirmUpdateFile();
        repositoryPage.openProjectsList().saveProject(projectName, "Updated " + OPENAPI_FILE_NAME);

        repositoryPage.openProjectsList().openProjectDetail(projectName).openRevisionByPosition(2);

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "MaxLimit");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(3, 1, "100");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName)
                .pickUpdateFile(OPENAPI_FILE_NAME, TestDataUtil.getFilePathFromResources(OPENAPI_FILE_3));
        assertThat(projectDetail.isUpdateFileNameWarningShown())
                .as("Warning should appear again when the replacement file has a different name")
                .isTrue();
        projectDetail.confirmUpdateFile();
        assertThat(projectDetail.isFilePresent(OPENAPI_FILE_NAME))
                .as("openapi.json should still be in the project after the update")
                .isTrue();
    }
}
