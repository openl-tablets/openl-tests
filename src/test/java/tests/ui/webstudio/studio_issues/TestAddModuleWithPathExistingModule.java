package tests.ui.webstudio.studio_issues;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;


import static org.assertj.core.api.Assertions.assertThat;

public class TestAddModuleWithPathExistingModule extends BaseTest {

    @Test
    @TestCaseId("EPBDS-11048")
    @Description("BUG: Two modules with the same path can be created")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAddModuleWithPathExistingModule() {
        // A module is written into the descriptor on the project's card, where the modules a project declares
        // by name are offered for writing; a project that declares none is drawn read-only, its modules being
        // found by the standard layout, and there two modules cannot read one workbook to begin with.
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, "CalcProject2.zip");
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repositoryPage.openProjectsList().openProjectDetail(projectName);

        String refusal = card.getOverviewTab().addDeclaredModuleExpectingRefusal("test", "CalcModule.xlsx");
        assertThat(refusal)
                .as("A module reading a workbook another module already reads must be refused")
                .contains("The path 'CalcModule.xlsx' is already read by another module.");
    }
}
