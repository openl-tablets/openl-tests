package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.projectdetail.ProjectOverviewTabComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.TestDataUtil;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectOverviewEditValidationUi extends BaseTest {

    private static final String TEMPLATE_NAME = "Example 2 - Corporate Rating";
    private static final String WORKBOOK_FILE = "Corporate Rating.xlsx";
    private static final String OPENAPI_FILE = "overview-pick-openapi.json";
    private static final long NO_AUTOSAVE_WINDOW_MS = 8000;

    @Test
    @TestCaseId({"EPBDS-16454", "EPBDS-16433"})
    @Description("Validates the EPBDS-16454 and EPBDS-16433 fixes on the Overview edit. An OpenAPI file picked from "
            + "the file system must stay in the unsaved edit - the Overview does not leave the edit mode and does "
            + "not write the project by itself, and Cancel drops the file. An unknown Properties processor class "
            + "and a version pattern with an unknown or a repeated property must be refused with a visible message "
            + "and leave the project unchanged.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testOverviewEditKeepsPickedOpenApiUnsavedAndRejectsInvalidSettings() {
        String projectName = "OverviewEditValidation_" + System.currentTimeMillis();

        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, projectName, TEMPLATE_NAME);

        ProjectDetailPage projectDetail = repositoryPage.openProjectsList().openProjectDetail(projectName);
        ProjectOverviewTabComponent overview = projectDetail.getOverviewTab();
        String revisionBefore = projectDetail.getOverviewRevision();
        String statusBefore = projectDetail.getStatus();

        overview.openForWriting();
        overview.pickOpenApiFile(TestDataUtil.getFilePathFromResources(OPENAPI_FILE));
        boolean editEnded = overview.isEditEndedWithin(NO_AUTOSAVE_WINDOW_MS);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(editEnded)
                .as("The Overview must stay in the edit mode after an OpenAPI file is picked")
                .isFalse();
        softly.assertThat(overview.isEditing())
                .as("Save must still be offered after an OpenAPI file is picked: nothing is saved until the user clicks it")
                .isTrue();
        softly.assertThat(overview.getPickedOpenApiPath())
                .as("The picked OpenAPI file must stay selected in the edit")
                .contains(OPENAPI_FILE);
        softly.assertThat(projectDetail.getStatus())
                .as("Picking an OpenAPI file must not change the project")
                .isEqualTo(statusBefore);
        softly.assertThat(projectDetail.getOverviewRevision())
                .as("Picking an OpenAPI file must not create a revision")
                .isEqualTo(revisionBefore);
        softly.assertAll();

        overview.cancelEditing();
        assertThat(projectDetail.getStatus())
                .as("Cancelling the edit must leave the project as it was")
                .isEqualTo(statusBefore);
        assertThat(projectDetail.isFilePresent(WORKBOOK_FILE))
                .as("The project files must be listed")
                .isTrue();
        assertThat(projectDetail.isFilePresent(OPENAPI_FILE))
                .as("The picked OpenAPI file must not be uploaded into the project before Save")
                .isFalse();

        overview = projectDetail.getOverviewTab();
        overview.openForWriting();
        SoftAssertions refusals = new SoftAssertions();
        overview.setPropertiesProcessor("aaa");
        refusals.assertThat(overview.saveExpectingRefusal())
                .as("An unknown Properties processor class must be refused with a message naming the processor")
                .contains("Invalid properties file name processor")
                .contains("'aaa'");
        refusals.assertThat(overview.isEditing())
                .as("The Overview must stay in the edit mode after the processor was refused")
                .isTrue();

        overview.setPropertiesProcessor("");
        overview.setVersionPattern("%unknownProp%");
        refusals.assertThat(overview.saveExpectingRefusal())
                .as("A version pattern with an unknown property must be refused with a message naming the property")
                .contains("Invalid properties file name pattern")
                .contains("unknownProp");

        overview.setVersionPattern("%lob%-%lob%");
        refusals.assertThat(overview.saveExpectingRefusal())
                .as("A version pattern repeating a property must be refused with a message naming the property")
                .contains("Invalid properties file name pattern")
                .contains("'lob'");
        refusals.assertAll();

        overview.cancelEditing();
        assertThat(projectDetail.getStatus())
                .as("The refused settings must not reach the project")
                .isEqualTo(statusBefore);
        assertThat(projectDetail.getOverviewRevision())
                .as("The refused settings must not create a revision")
                .isEqualTo(revisionBefore);
    }
}
