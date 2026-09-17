package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestWithinCurrentModuleOnlyAfterModuleSwitch extends BaseTest {

    private static final String TEMPLATE = "Example 3 - Auto Policy Calculation";

    @Test
    @TestCaseId("EPBDS-16653")
    @Description("The tests launcher of a module whose project compiles offers 'Within Current Module Only' "
            + "to be chosen. Fails on EPBDS-16653: the box is locked after the reader moves from one module "
            + "of the project to another, although nothing in the project is in error.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue("EPBDS-16653")
    public void testWithinCurrentModuleOnlyStaysOfferedAfterSwitchingModule() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE);
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "AutoPolicyCalculation");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("The box is offered on the module the reader opened first")
                .isTrue();

        // Moving to the other module of the same project and back again. Nothing is in error and the
        // project is compiled through, so the box is offered on every module the reader lands on.
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "AutoPolicyTests");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("The box should be offered on the module the reader moved to")
                .isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "AutoPolicyCalculation");
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("The box should still be offered on a module the reader comes back to")
                .isTrue();
    }
}
