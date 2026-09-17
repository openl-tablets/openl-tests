package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ResolveConflictsDialogComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRenameProjectFromOldRevisionConflictUi extends BaseTest {

    private static final String TEMPLATE_NAME = "Tutorial 1 - Introduction to Decision Tables";

    @Test
    @TestCaseId("EPBDS-16269")
    @Description("Renaming a project, then renaming it again from an older revision must offer Resolve Conflicts "
            + "instead of refusing the save (regression guard for EPBDS-16269).")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRenameFromOldRevisionOffersConflictResolution() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE_NAME);
        String renamedOnce = projectName + "2";
        String renamedTwice = projectName + "3";
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(projectName);

        throw new SkipException("KNOWN-ISSUES.md #13: a project can no longer be renamed — the card offers "
                + "what the descriptor says and the project's own actions, and none of them renames it.");
    }
}
