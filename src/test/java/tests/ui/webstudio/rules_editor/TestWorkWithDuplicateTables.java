package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.components.editortabcomponents.leftmenu.EditorLeftRulesTreeComponent;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static domain.ui.webstudio.components.editortabcomponents.leftmenu.TableTypeFolders.DECISION;

public class TestWorkWithDuplicateTables extends BaseTest {

    private static final String NAME_PROJECT_SAME_MODULE = "Error_in_table-same_module";
    private static final String NAME_PROJECT_DIFF_MODULES = "Error_in_table-diff_modules";
    private static final String NAME_PROJECT_WITH_DEPENDENCY = "Error_in_table-project_with_dependency";
    private static final String NAME_PROJECT_DEPENDENT = "Project2";

    @Test
    @TestCaseId("IPBQA-31790")
    @Description("Work With Duplicate Tables - error messages, Run/Trace/Benchmark with WithinCurrentModuleOnly checkboxes")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testWorkWithDuplicateTables() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));

        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                NAME_PROJECT_SAME_MODULE, NAME_PROJECT_SAME_MODULE + ".zip");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(NAME_PROJECT_SAME_MODULE, "module_AZ");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolderRaisingErrors(DECISION, "someLookupBig2", true);
        assertThat(editorPage.getProblemsPanelComponent().errorsSaying("Found duplicated table 'SmartLookup Double someLookupBig2( String param1, Integer param2)'."))
                .as("Error message for duplicated table in same module")
                .contains("Found duplicated table 'SmartLookup Double someLookupBig2( String param1, Integer param2)'.");
        editorPage.getCenterTable().editCell(3, 1, "Param 2");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorTableActionsPanelComponent().closeTableEditor();
        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("Error message should persist after editing cell")
                .contains("Found duplicated table 'SmartLookup Double someLookupBig2( String param1, Integer param2)'.");

        assertThat(editorPage.getEditorToolbarPanelComponent().isRunButtonVisible())
                .as("Run button should be absent for table with error")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTraceButtonVisible())
                .as("Trace button should be absent for table with error")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isAvailableTestRunsLinkVisible())
                .as("AvailableTestRunsLink should be absent for table with error")
                .isFalse();

        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolderRaisingErrors(DECISION, "someLookupBig2", false);
        editorPage.getEditorToolbarPanelComponent().clickRun();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be unchecked after Run click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be enabled after Run click (same module)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be unchecked after Trace click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be enabled after Trace click (same module)")
                .isTrue();
        editorPage.refresh();
        editorPage.getEditorToolbarPanelComponent().clickTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyTestTablesChecked())
                .as("WithinCurrentModuleOnly should be unchecked after TestDropdown click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyTestTablesEnabled())
                .as("WithinCurrentModuleOnly should be enabled after TestDropdown click (same module)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().getAvailableTestRunsLinkText())
                .as("AvailableTestRunsLink text for duplicate table without error")
                .contains("someLookupBig2Test (1 test case)");
        editorPage.getEditorToolbarPanelComponent().clickTableActionsTestBtn();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "someLookupBig2Test");
        assertThat(editorPage.getEditorMainContentProblemsPanelComponent().isErrorMessageListPresent())
                .as("No errors should be shown for test table in same module")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickRunDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked after RunDropdown click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled after RunDropdown click (same module)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked after Trace click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled after Trace click (same module)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickBenchmarkDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked after BenchmarkDropdown click (same module)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled after BenchmarkDropdown click (same module)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                NAME_PROJECT_DIFF_MODULES, NAME_PROJECT_DIFF_MODULES + ".zip");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(NAME_PROJECT_DIFF_MODULES, "module_AZ");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "someLookupBig2");
        assertThat(editorPage.getProblemsPanelComponent().errorsSaying("There can be only one active table."))
                .as("Error message for duplicate table in different modules")
                .contains("There can be only one active table.");
        editorPage.getCenterTable().editCell(3, 1, "Param 2");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorTableActionsPanelComponent().closeTableEditor();
        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("Error message should persist after editing cell (diff modules)")
                .contains("There can be only one active table.");

        editorPage.getEditorToolbarPanelComponent().clickRun();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be checked (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be disabled (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be checked after Trace (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be disabled after Trace (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickRun()
                .setInputTextField("1", "a1")
                .setInputTextField("2", "11")
                .clickRunInsideMenu();
        assertThat(editorPage.getTestResultValidationComponent().getResultTable().getCellText(1, 3))
                .as("Run result for someLookupBig2 should be 100")
                .isEqualTo("100");

        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder(DECISION, "someLookupBig2");
        editorPage.getEditorToolbarPanelComponent().clickTableActionsTestDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyTestTablesChecked())
                .as("WithinCurrentModuleOnly should be checked in TestDropdown (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyTestTablesEnabled())
                .as("WithinCurrentModuleOnly should be disabled in TestDropdown (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickTableActionsTestBtn();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorLeftRulesTreeComponent().selectItemInFolder(DECISION, "someLookupBig2");
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        editorPage.getEditorToolbarPanelComponent().setTopPanelWithinCurrentModuleOnly(false);
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyChecked())
                .as("TopPanel WithinCurrentModuleOnly should be unchecked (diff modules)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTopPanelWithinCurrentModuleOnlyEnabled())
                .as("TopPanel WithinCurrentModuleOnly should be enabled (diff modules)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().runAllTests();
        editorPage.getTestResultValidationComponent().checkTestTableFailed("someLookupBig2Test");
        editorPage.getEditorToolbarPanelComponent().clickTopPanelTestDropdown();
        editorPage.getEditorToolbarPanelComponent().setTopPanelWithinCurrentModuleOnly(true);
        editorPage.getEditorToolbarPanelComponent().clickTopPanelRunTestBtn();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "someLookupBig2Test");
        assertThat(editorPage.getEditorMainContentProblemsPanelComponent().getWarningMessages())
                .as("Error message for test table referencing duplicated rule (diff modules)")
                .contains("Tested rules have errors");
        editorPage.getEditorToolbarPanelComponent().clickRunDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be checked (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be disabled (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be checked after Trace (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be disabled after Trace (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickBenchmarkDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be checked after BenchmarkDropdown (diff modules)")
                .isTrue();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be disabled after BenchmarkDropdown (diff modules)")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                NAME_PROJECT_WITH_DEPENDENCY, NAME_PROJECT_WITH_DEPENDENCY + ".zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                NAME_PROJECT_DEPENDENT, NAME_PROJECT_DEPENDENT + ".zip");

        editorPage = new EditorPage();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectModule(NAME_PROJECT_WITH_DEPENDENCY, "module_AZ");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "someLookupBig2");
        assertThat(editorPage.getProblemsPanelComponent().errorsSaying("Method 'someLookupBig2(String param1," +
                        " Integer param2)' is already used in modules 'module_AZ' and 'module_KS' with the same version," +
                        " active status, properties set."))
                .as("Error message for duplicate table across projects with dependency")
                .contains("Method 'someLookupBig2(String param1," +
                        " Integer param2)' is already used in modules 'module_AZ' and 'module_KS' with the same version," +
                        " active status, properties set.");
        editorPage.getCenterTable().editCell(3, 1, "Param 2");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorTableActionsPanelComponent().closeTableEditor();
        assertThat(editorPage.getProblemsPanelComponent().getAllErrors())
                .as("Error message should persist after editing cell (dependency project)")
                .contains("Method 'someLookupBig2(String param1," +
                        " Integer param2)' is already used in modules 'module_AZ' and 'module_KS' with the same version," +
                        " active status, properties set.");

        assertThat(editorPage.getEditorToolbarPanelComponent().isRunButtonVisible())
                .as("Run button should be absent for table with error (dependency project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isTraceButtonVisible())
                .as("Trace button should be absent for table with error (dependency project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isAvailableTestRunsLinkVisible())
                .as("AvailableTestRunsLink should be absent for table with error (dependency project)")
                .isFalse();

        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree("Test")
                .selectItemInFolder("Test", "someLookupBig2Test");
        assertThat(editorPage.getEditorMainContentProblemsPanelComponent().isErrorMessageListPresent())
                .as("No errors should be shown for test table in dependency project")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickRunDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked (dependency project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled (dependency project)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked after Trace (dependency project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled after Trace (dependency project)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickBenchmarkDropdown();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnlyTestTables should be unchecked after BenchmarkDropdown (dependency project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnlyTestTables should be enabled after BenchmarkDropdown (dependency project)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickRun().clickRunInsideMenu();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();
        editorPage.getEditorToolbarPanelComponent().runAllTests();
        editorPage.getTestResultValidationComponent().checkAllTablesPassed();

        editorPage.getEditorToolbarPanelComponent().selectBreadcrumbModule(NAME_PROJECT_DEPENDENT, "module_KS");
        editorPage.getEditorLeftRulesTreeComponent()
                .setViewFilter(EditorLeftRulesTreeComponent.FilterOptions.BY_TYPE)
                .expandFolderInTree(DECISION)
                .selectItemInFolder(DECISION, "someLookupBig2");
        assertThat(editorPage.getEditorMainContentProblemsPanelComponent().isErrorMessageListPresent())
                .as("No errors should be shown for decision table in dependent project")
                .isFalse();
        editorPage.getEditorToolbarPanelComponent().clickRun();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be unchecked (dependent project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be enabled (dependent project)")
                .isTrue();
        editorPage.getEditorToolbarPanelComponent().clickTrace();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsChecked())
                .as("WithinCurrentModuleOnly should be unchecked after Trace (dependent project)")
                .isFalse();
        assertThat(editorPage.getEditorToolbarPanelComponent().isWithinCurrentModuleOnlyInputArgsEnabled())
                .as("WithinCurrentModuleOnly should be enabled after Trace (dependent project)")
                .isTrue();
    }
}
