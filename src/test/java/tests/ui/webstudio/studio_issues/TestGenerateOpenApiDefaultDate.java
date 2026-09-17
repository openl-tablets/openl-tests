package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGenerateOpenApiDefaultDate extends BaseTest {

    @Test
    @TestCaseId("EPBDS-10789")
    @Description("After clicking 'Create or Update Schema' in Import OpenAPI dialog, openapi.json appears in the repository tree")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testGenerateOpenApiDefaultDate() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN,
                "StudioIssues.TestGenerateOpenApiDefaultDate.zip");

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        // A project whose workbooks lie in its root holds no descriptor, and the settings the schema is
        // written from are offered once they are moved under rules/.
        editorPage.migrateProject();

        // The card offers to write the specification of the rules beside its OpenAPI heading, as it is read;
        // the settings the card is written through hold no such action.
        editorPage.writeOpenApiSchema();

        // Navigate to Repository tab and verify openapi.json appears in the project tree
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        assertThat(repositoryPage.openProjectsList().openProjectDetail(projectName).isFilePresent("openapi.json"))
                .as("openapi.json should appear in the repository tree after generating the OpenAPI schema")
                .isTrue();
    }

    @Test
    @TestCaseId("EPBDS-16656")
    @Description("The card names the specification a generation has just written. Fails on EPBDS-16656: the "
            + "OpenAPI section goes on saying the project declares none until the card is drawn again.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16656")
    public void testCardNamesTheSpecificationJustGenerated() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN,
                "Example 3 - Auto Policy Calculation");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        editorPage.writeOpenApiSchema();

        assertThat(editorPage.isOpenApiPropertiesSectionEmpty())
                .as("The card should name the specification the generation has just written")
                .isFalse();
        assertThat(editorPage.getOpenApiPropertyValue("File"))
                .as("The card should name the file the generation wrote")
                .isEqualTo("openapi.json");
    }
}
