package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.KnownIssue;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.WorkflowService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestTabRevisionsInEditor extends BaseTest {

    @Test
    @TestCaseId({"IPBQA-30123", "EPBDS-12242"})
    @Description("Verifies revisions tab in editor: initial revision after project creation, new revision after save, and opening old revision shows original cell value. "
            + "EPBDS-12242: the revision of a project copied as new says Copied from: <project>. with the source project as a link in the Revisions tab that opens that project. "
            + "Fails on EPBDS-17171: the link was removed by EPBDS-16945.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    @KnownIssue(value = "EPBDS-17171", failsWith = "The source project in the Copied from comment should be a link")
    public void testTabRevisionsInEditor() {
        String projectName = WorkflowService.loginCreateProjectFromTemplate(User.ADMIN, "Sample Project");
        EditorPage editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();

        EditorRevisionsTabComponent revisionsTab = new EditorRevisionsTabComponent();
        revisionsTab.waitForTableToLoad();
        assertThat(revisionsTab.getRowCount())
                .as("Initial revision count should be 1")
                .isEqualTo(1);
        assertThat(revisionsTab.getCommentForRow(1))
                .as("Initial revision comment should indicate project creation")
                .isEqualTo("Project " + projectName + " is created.");

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");

        editorPage.getEditorToolbarPanelComponent().getEditTableBtn().click();
        editorPage.getCenterTable().editCell(6, 2, "100");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();

        editorPage.reloadPage();
        editorPage = new EditorPage();
        editorPage.getEditorToolbarPanelComponent().clickSave();
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        editorPage.reloadPage();
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();

        revisionsTab.waitForTableToLoad();
        assertThat(revisionsTab.getRowCount())
                .as("Revision count after save should be 2")
                .isEqualTo(2);
        assertThat(revisionsTab.getCommentForRow(1))
                .as("Most recent revision comment should indicate project save")
                .isEqualTo("Project " + projectName + " is saved.");
        assertThat(revisionsTab.getCommentForRow(2))
                .as("Oldest revision comment should indicate project creation")
                .isEqualTo("Project " + projectName + " is created.");

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        int repositoryRevisionCount = repositoryPage.openProjectDetail(projectName).getRevisionsCount();

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();
        revisionsTab.waitForTableToLoad();
        assertThat(revisionsTab.getRowCount())
                .as("Editor revisions count should match repository revisions count")
                .isEqualTo(repositoryRevisionCount);

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();

        revisionsTab.waitForTableToLoad();
        assertThat(revisionsTab.getRowCount())
                .as("Revision count should still be 2 after module navigation")
                .isEqualTo(2);

        editorPage.reloadPage();
        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.closeProject(projectName);
        repositoryPage.waitUntilSpinnerLoaded();
        repositoryPage.openProject(projectName);
        repositoryPage.waitUntilSpinnerLoaded();
        repositoryPage.waitUntilAppIdle();
        editorPage = new EditorPage();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorToolbarPanelComponent().clickMore().clickRevisions();

        revisionsTab.waitForTableToLoad();
        revisionsTab.openRevision(2);

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(projectName, "Main");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "Hello");

        assertThat(editorPage.getCenterTable().getCellText(6, 2))
                .as("Cell value should be the original value before the edit when viewing old revision")
                .isEqualTo("0");

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        String sourceId = repositoryPage.openProjectsList().openProjectDetail(projectName).getProjectId();
        String copyName = projectName + "Copy";
        repositoryPage.openProjectsList().copyProject(projectName, copyName);
        ProjectDetailPage copy = repositoryPage.openProjectsList().openProjectDetail(copyName);
        assertThat(copy.getRevisionDescriptions())
                .as("The first revision of the copy should say where it was copied from")
                .containsExactly("Copied from: " + projectName + ".");
        assertThat(copy.isRevisionCommentLinkOffered(projectName))
                .as("The source project in the Copied from comment should be a link (EPBDS-12242)")
                .isTrue();
        assertThat(copy.clickRevisionCommentLink(projectName).getProjectId())
                .as("The link in the Copied from comment should open the source project (EPBDS-12242)")
                .isEqualTo(sourceId);
    }
}
