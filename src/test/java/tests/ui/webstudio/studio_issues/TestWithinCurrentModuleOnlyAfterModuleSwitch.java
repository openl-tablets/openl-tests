package tests.ui.webstudio.studio_issues;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
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
    @Description("The tests launcher offers 'Within Current Module Only' on the module the reader moved to "
            + "through the module list of the breadcrumbs, which is the fix of EPBDS-16653 this test guards.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
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

        editorPage.getEditorToolbarPanelComponent().selectBreadcrumbModule(projectName, "AutoPolicyTests");
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("The box should be offered on the module the reader moved to")
                .isTrue();

        editorPage.getEditorToolbarPanelComponent().selectBreadcrumbModule(projectName, "AutoPolicyCalculation");
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("The box should still be offered on a module the reader comes back to")
                .isTrue();
    }
}
