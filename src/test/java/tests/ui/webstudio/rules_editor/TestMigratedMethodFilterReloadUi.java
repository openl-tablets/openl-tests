package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMigratedMethodFilterReloadUi extends BaseTest {

    private static final String TEMPLATE_NAME = "Sample Project";
    private static final String MODULE_NAME = "Main";
    private static final String TABLE_NAME = "Hello";
    private static final int RELOAD_SETTLE_TIMEOUT_MS = 30000;

    @Test
    @TestCaseId("EPBDS-16275")
    @Description("A project whose module declares a method filter of its own reloads once and settles: the "
            + "loading overlay must reach a quiet window and the table must stay. Guards the reload loop of "
            + "EPBDS-16275, which the JSF shell fell into after its ViewState was evicted; the filter is "
            + "written into the descriptor, which is where a module's own filter is declared and where the "
            + "card reads it from, the screen offering no form of its own for it.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testProjectReloadAfterMethodFilterMigration() throws IOException {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, TEMPLATE_NAME);
        EditorPage editorPage = new EditorPage();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        ProjectDetailPage card = repositoryPage.openProjectsList().openProjectDetail(projectName);
        card.updateFile("rules.xml", descriptorDeclaringAMethodFilter(projectName));

        assertThat(card.getOverviewTab().moduleMethodFilter("rules/" + MODULE_NAME + ".xlsx"))
                .as("The card should show the method filter the module declares")
                .contains("a").contains("b");

        EditorPage editor = new EditorPage();
        editor.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, MODULE_NAME);
        editor.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Rules")
                .selectItemInFolder("Rules", TABLE_NAME);

        editor.getEditorToolbarPanelComponent().clickProjectRefresh();

        assertThat(editor.waitUntilAppIdle())
                .as("The loading overlay must settle after a project reload; an overlay that never leaves "
                        + "is the EPBDS-16275 reload loop")
                .isTrue();

        boolean tableSettled = WaitUtil.waitForCondition(() -> editor.getCenterTable().isVisible(),
                RELOAD_SETTLE_TIMEOUT_MS, 500, "Waiting for the reloaded module's table to settle");
        assertThat(tableSettled)
                .as("The module must load once after a project reload instead of reloading endlessly")
                .isTrue();

        assertThat(editor.waitUntilAppIdle())
                .as("The app must stay idle once the reload finished - a re-appearing overlay means the "
                        + "reload loop resumed")
                .isTrue();
        assertThat(editor.getCenterTable().isVisible())
                .as("The table must stay on screen once the reload finished")
                .isTrue();
    }

    /**
     * The descriptor of the project with a method filter written into the module it declares. The project
     * name is what the descriptor is refused without, so it is written in as the project was created.
     */
    private static String descriptorDeclaringAMethodFilter(String projectName) throws IOException {
        String descriptor = "<project>\n"
                + "    <name>" + projectName + "</name>\n"
                + "    <modules>\n"
                + "        <module>\n"
                + "            <name>" + MODULE_NAME + "</name>\n"
                + "            <rules-root path=\"rules/" + MODULE_NAME + ".xlsx\"/>\n"
                + "            <method-filter>\n"
                + "                <includes>\n"
                + "                    <value>a</value>\n"
                + "                </includes>\n"
                + "                <excludes>\n"
                + "                    <value>b</value>\n"
                + "                </excludes>\n"
                + "            </method-filter>\n"
                + "        </module>\n"
                + "    </modules>\n"
                + "</project>\n";
        Path written = Files.createTempFile("rules-with-method-filter", ".xml");
        Files.writeString(written, descriptor, StandardCharsets.UTF_8);
        return written.toAbsolutePath().toString();
    }
}
