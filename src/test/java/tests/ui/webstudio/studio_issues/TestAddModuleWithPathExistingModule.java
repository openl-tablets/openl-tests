package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.BaseTest;


import static org.assertj.core.api.Assertions.assertThat;

public class TestAddModuleWithPathExistingModule extends BaseTest {

    private static final boolean ADDING_A_MODULE_IS_BLOCKED = true;

    @Test
    @TestCaseId("EPBDS-11048")
    @Description("BUG: Two modules with the same path can be created")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAddModuleWithPathExistingModule() {
        if (ADDING_A_MODULE_IS_BLOCKED) {
            throw new SkipException("KNOWN-ISSUES.md #8: a module can no longer be added from the project's "
                    + "card — the panel lists the modules read-only and says to put the workbook in the rules "
                    + "folder, so there is no form left to write a path into.");
        }
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);
        editorPage.getProjectDetailsComponent().openAddModulePopup();
        editorPage.getAddModulePopupComponent().fillForm("test", "Main.xlsx");
        assertThat(editorPage.getAddModulePopupComponent().isSpecificPropertyShown("Path is already covered with existing module.")).isTrue().as("'Path is already covered with existing module.' text is expected to be shown");
    }
}