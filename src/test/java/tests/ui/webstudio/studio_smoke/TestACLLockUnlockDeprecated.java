package tests.ui.webstudio.studio_smoke;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorToolbarPanelComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestACLLockUnlockDeprecated extends BaseTest {

    private static final String LOCK = "lock";
    private static final String UNLOCK = "unlock";

    @Test
    @TestCaseId("EPBDS-15712")
    @Description("ACL: Lock/Unlock buttons are deprecated — not present in Repository tab or Editor toolbar")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testLockUnlockNotPresentInUI() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Example 1 - Bank Rating");

        EditorPage editorPage = new EditorPage();

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        List<String> rowActions = repositoryPage.getProjectActionLabels(projectName);
        assertThat(rowActions)
                .as("Repository row actions should not contain Lock or Unlock (BRD TR2). Actual actions: %s", rowActions)
                .noneMatch(action -> action.toLowerCase().contains(LOCK) || action.toLowerCase().contains(UNLOCK));

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent()
                .selectModule(projectName, "Bank Rating");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "CapitalDynamicScore");

        EditorToolbarPanelComponent toolbar = editorPage.getEditorToolbarPanelComponent();
        List<String> toolbarActions = toolbar.getAllVisibleTopToolbarActions();
        assertThat(toolbarActions)
                .as("Editor toolbar should not contain Lock or Unlock (BRD TR2). Actual actions: %s", toolbarActions)
                .noneMatch(action -> action.toLowerCase().contains(LOCK) || action.toLowerCase().contains(UNLOCK));

        List<String> moreMenuItems = toolbar.getMoreMenuItems();
        assertThat(moreMenuItems)
                .as("Editor More menu should not contain Lock or Unlock (BRD TR2). Actual items: %s", moreMenuItems)
                .noneMatch(item -> item.toLowerCase().contains(LOCK) || item.toLowerCase().contains(UNLOCK));
    }
}
