package tests.ui.webstudio.rules_editor;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.time.Duration;

import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;
import static org.assertj.core.api.Assertions.assertThat;

public class TestEditorRefreshWhileCompilingUi extends BaseTest {

    private static final String TEMPLATE_NAME = "Tutorial 1 - Introduction to Decision Tables";
    private static final String MODULE_NAME = "Tutorial1 - Intro to Decision Tables";
    private static final String FIRST_TABLE = "DriverPremium1";
    private static final String SECOND_TABLE = "DriverPremium2";
    private static final int ROUNDS = 4;
    private static final int REFRESHES_PER_ROUND = 3;
    private static final Duration RESPONSIVE_WITHIN = Duration.ofSeconds(30);

    @Test
    @TestCaseId("EPBDS-16898")
    @Description("EPBDS-16898: Refresh pressed three times in a row resets the module while the project is still "
            + "compiling. The Editor must stay responsive: the next table opens within seconds, it did not "
            + "freeze for about a minute until the reset gave up waiting for the compilation.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void editorStaysResponsiveWhenRefreshedWhileCompiling() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE_NAME);
        EditorPage editor = new EditorPage();
        editor.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        selectTable(editor, FIRST_TABLE);
        EditorToolbarPanelComponent toolbar = editor.getEditorToolbarPanelComponent();

        for (int round = 1; round <= ROUNDS; round++) {
            String next = round % 2 == 1 ? SECOND_TABLE : FIRST_TABLE;
            long started = System.nanoTime();
            toolbar.clickProjectRefreshInBurst(REFRESHES_PER_ROUND);
            selectTable(editor, next);
            assertThat(editor.getCenterTable().isVisible())
                    .as("Round " + round + ": the table " + next + " is shown after the module was refreshed")
                    .isTrue();
            Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
            LOGGER.info("EPBDS-16898 round {}: {} refreshes and the next table took {} ms", round, REFRESHES_PER_ROUND, elapsed.toMillis());
            assertThat(elapsed)
                    .as("Round " + round + ": the Editor answers within " + RESPONSIVE_WITHIN.toSeconds()
                            + " s after Refresh pressed " + REFRESHES_PER_ROUND + " times while compiling")
                    .isLessThan(RESPONSIVE_WITHIN);
        }
    }

    private void selectTable(EditorPage editor, String tableName) {
        editor.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, tableName);
    }
}
