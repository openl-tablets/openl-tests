package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.OpenApiSpecificationComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGenerateOpenApiDefaultDate extends BaseTest {

    private static final int CARD_SETTLE_MS = 5000;

    @Test
    @TestCaseId("EPBDS-10789")
    @Description("After clicking 'Create or Update Schema' in Import OpenAPI dialog, openapi.json appears in the repository tree")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testGenerateOpenApiDefaultDate() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN,
                "StudioIssues.TestGenerateOpenApiDefaultDate.zip");

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        editorPage.migrateProject();

        editorPage.writeOpenApiSchema();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(projectName).isFilePresent("openapi.json"))
                .as("openapi.json should appear in the repository tree after generating the OpenAPI schema")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16656")
    @Description("The card names the specification a generation has just written.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testCardNamesTheSpecificationJustGenerated() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN,
                "Example 3 - Auto Policy Calculation");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        assertThat(editorPage.isOpenApiPropertiesSectionEmpty())
                .as("Precondition: the card should say the project declares no specification before the generation")
                .isTrue();

        new OpenApiSpecificationComponent().generateSpecification();
        WaitUtil.waitForCondition(() -> !editorPage.isOpenApiPropertiesSectionEmpty(), CARD_SETTLE_MS, 250,
                "Waiting for the card to read the project back after the generation");

        assertThat(editorPage.isOpenApiPropertiesSectionEmpty())
                .as("The card should name the specification the generation has just written")
                .isFalse();
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("The card should name the file the generation wrote")
                .isEqualTo("openapi.json");
    }
}
