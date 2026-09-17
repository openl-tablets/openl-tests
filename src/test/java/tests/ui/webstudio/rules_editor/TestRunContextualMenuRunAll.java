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

/**
 * Covers the EPBDS-14039 delta only ("Run All" checkbox + new labels + info-icon tooltip move)
 * out of the broader Run Contextual Menu spec described in EPBDS-15969.
 *
 * The full spec includes many sections that this class deliberately does NOT cover.
 * If/when those sections get TAF coverage, the new assertions MUST land in this class
 * (one home for the whole contextual menu), not in a separate test.
 *
 * Sections NOT yet covered here:
 *   A — toolbar entry points: Test/Run/Trace/Benchmark buttons rendering, arrow-on-hover,
 *       body-click semantics (Run body runs all, Test body skips validation, Trace body opens
 *       the menu, Benchmark body runs), single-instance #unitsMenu with .b-run/.b-test/.b-trace/.b-benchmark
 *       footer filtering.
 *   B.2–B.8 — checkbox list selection (≤20 cases):
 *       check/uncheck all toggle, header checkbox auto-desync on row uncheck, selected subset
 *       → request with comma-joined testRanges, "All" mode does NOT send testRanges param,
 *       swap range ↔ table via "Use the Range", selector shared across Test/Run/Run into File/
 *       Test into File/Trace/Trace into File/Benchmark, "Run Cases" header for xls.run.method tables.
 *   C.2–C.4, C.6–C.8 — range-input (>20 cases):
 *       range input pre-fills with the first test id, default execution uses pre-filled range,
 *       accepted range syntax (2-4,7,10-12 / id3-id7 / 1,3,5 / 1-100 / trailing comma),
 *       validation errors via isAnyTestSelected (`Wrong [..] ID in the Range of IDs`,
 *       `No tests selected`), range applies to all menu actions, performance smoke (regression
 *       cover for EPBDS-13816 — large table opens fast without 100-row DOM).
 *   D — "Within Current Module Only" checkbox (#runTestModuleOnly):
 *       default unchecked when project compiles cleanly, user toggle reaches the server
 *       (currentOpenedModule=true|false), confirm dialog when project is NOT compiled,
 *       confirm dialog when other modules have errors (tableHasProblems), flag applies
 *       to all menu actions.
 *   E — runCasesSettings panel (Test tables only, NOT for xls.run.method):
 *       panel renders for Test tables and is hidden for Run tables, "Failures Only" toggles
 *       the "Failures per test" dropdown visibility, settings flow through to the results page,
 *       panel state shared across actions in the same session.
 *   F — footer buttons behaviour:
 *       Run / Run into File / Test / Test into File honor selection (download via ws.nav.download),
 *       Trace / Trace into File (startTestTableTrace + download), Benchmark navigates to
 *       studio.url('test/benchmark'), empty selection blocks every action ("No tests selected").
 *   G — state preservation across menu reopens and context switches:
 *       reopening preserves checkbox/range state, switching action context keeps the same
 *       selection (single #testSelector shared via itemStatuses.switchTo), trace-separate
 *       selection baseline (EPBDS-11675), menu closes on outside click and on action invocation.
 *   H — Failures Only / Compound Result UX:
 *       settings-page default applied on first menu open, per-session overrides do not change
 *       admin defaults.
 *   I.1–I.4, I.6 — edge cases:
 *       special characters in test ids (escape regression for EPBDS-13846), concurrent edits
 *       on the same module while menu is open, boundary exactly at 20 cases (le 20 inclusive),
 *       test with zero test cases, console error scan (no jQuery deprecation warnings —
 *       regression cover for EPBDS-14407, no RichFaces AJAX errors).
 */
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
        // ============ Setup: import a project with a 25-case Test table (doubleItTest) and a 5-case (smallTest) ============
        String projectName = WorkflowService.loginCreateProjectFromZip(User.ADMIN, ZIP_NAME);

        EditorPage editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        editorPage.getProblemsPanelComponent().waitForCompilationToComplete();

        // ============ STEP 1: Open the >20-case Test table ============
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", BIG_TEST);

        // ============ STEP 2: the launcher takes every case unless the reader picks some ============
        editorPage.getEditorToolbarPanelComponent().clickRun();
        RunMenuComponent launcher = editorPage.getEditorToolbarPanelComponent().getRunLauncher();
        launcher.waitForLauncher();

        assertThat(launcher.isAllCasesOffered())
                .as("C.9 — a test table of several cases must offer to take them all")
                .isTrue();
        assertThat(launcher.isAllCasesChecked())
                .as("C.9 — every case is taken unless the reader says otherwise")
                .isTrue();
        // The cases are drawn a page at a time, twenty-five to a page, and this table holds exactly that.
        assertThat(launcher.getDrawnCaseCount())
                .as("C.1 — the launcher must list every case of the table")
                .isEqualTo(25);

        // ============ STEP 3: picking one case is saying otherwise ============
        launcher.pickFirstCase();
        assertThat(launcher.isAllCasesChecked())
                .as("C.10 — picking a case must stop every case being taken")
                .isFalse();

        // ============ STEP 4: taking them all again drops what was picked ============
        launcher.setAllCases(true);
        assertThat(launcher.isAllCasesChecked())
                .as("C.10 — taking them all again must stand")
                .isTrue();

        // The range the cases could be written by, the note about it and the refusal to list more than
        // twenty of them one by one are gone from the screen — see KNOWN-ISSUES.md #18.
    }
}
