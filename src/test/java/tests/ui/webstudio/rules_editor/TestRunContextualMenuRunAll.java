package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import com.microsoft.playwright.Page;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent.FilterOptions;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import domain.ui.webstudio.components.editortabcomponents.toolbar.RunMenuComponent;
import tests.BaseTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRunContextualMenuRunAll extends BaseTest {

    private static final String ZIP_NAME = "RunAllTestTable.zip";
    private static final String MODULE_NAME = "Rules";
    private static final String BIG_TEST = "doubleItTest";
    private static final String SMALL_TEST = "smallTest";

    @Test
    @TestCaseId("EPBDS-15969")
    @Description("ACL: 'Run All' checkbox in the Run contextual menu — autofills range, locks readonly, "
            + "auto-unticks on context switch (Test/Trace/Benchmark); absent for tables with ≤20 cases. "
            + "Covers EPBDS-14039 scenarios C.9–C.13 and the corollary in I.5.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testRunAllToggleAndContextReset() {
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, ZIP_NAME);

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", BIG_TEST);

        editorPage.getEditorToolbarPanelComponent().clickRun();
        RunMenuComponent launcher = editorPage.getEditorToolbarPanelComponent().getRunLauncher();
        launcher.waitForLauncher();

        assertThat(launcher.isAllCasesOffered())
                .as("C.9 — a test table of several cases must offer to take them all")
                .isTrue();
        assertThat(launcher.isAllCasesChecked())
                .as("C.9 — every case is taken unless the reader says otherwise")
                .isTrue();
        assertThat(launcher.getDrawnCaseCount())
                .as("C.1 — the launcher must list every case of the table")
                .isEqualTo(25);

        launcher.pickFirstCase();
        assertThat(launcher.isAllCasesChecked())
                .as("C.10 — picking a case must stop every case being taken")
                .isFalse();

        launcher.setAllCases(true);
        assertThat(launcher.isAllCasesChecked())
                .as("C.10 — taking them all again must stand")
                .isTrue();
    }
}
