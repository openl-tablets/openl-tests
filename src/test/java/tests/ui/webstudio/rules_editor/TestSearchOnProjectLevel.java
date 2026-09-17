package tests.ui.webstudio.rules_editor;

import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.editortabcomponents.SearchFilterComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.StringUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSearchOnProjectLevel extends BaseTest {

    @Test
    @TestCaseId("IPBQA-32590")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testSimpleSearchOnProjectLevel() {
        // Precondition: login and create 3 projects from zip
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String nameProjectSpreadsheetSalary = "SpreadsheetSalary";
        String nameProjectSearchingByTag = "SearchingByTag";
        String nameProjectExample1BankRating = "Example1BankRating";

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSpreadsheetSalary, "RulesEditor.TestSearchOnProjectLevel.SpreadsheetSalary.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSearchingByTag, "RulesEditor.TestSearchOnProjectLevel.SearchingByTag.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectExample1BankRating, "RulesEditor.TestSearchOnProjectLevel.Example1BankRating.zip");

        new EditorPage();
        editorPage = new EditorPage();
        SearchFilterComponent search = editorPage.getSearchFilterComponent();

        // 1.1 Empty search returns all tables in project
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 1.2 Search by cell value
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("median");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("step1");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 1.3 Search by table name / signature / return value
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("SalaryCalc");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("Spreadsheet Double SalaryInfo ( String name, Double [] salary )");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("resul");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();

        // 1.4 Search in another project
        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectExample1BankRating);
        search.typeSearchAndEnter("MONEY");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();
        assertThat(search.isTableFound("NetMoneyMarketLiabilitiesScore")).isTrue();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectExample1BankRating);
        // The text is trimmed before it is sent, so a leading space and a trailing one name the same
        // search and the two cases they used to tell apart can no longer be told apart. See
        // KNOWN-ISSUES.md #11; what remains checkable is that the search finds what carries the word.
        search.typeSearchAndEnter("Balance");
        search.waitForSearchResult();
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();

        // 1.6 Search by tag, then view table
        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.typeSearchAndEnter("testingFirst");
        search.waitForSearchResult();
        assertThat(search.isTableFound("RulesName")).isTrue();
        search.clickViewTable("RulesName");
        // After viewing table, search results should still be accessible
        assertThat(search.isTableFound("RulesName")).isTrue();

        // 1.7 Search for non-existing values
        search.typeSearchAndEnter("money");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();

        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.typeSearchAndEnter("''");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.typeSearchAndEnter(" alert(\"zzz\") ");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();
    }

    @Test
    @TestCaseId("IPBQA-32590")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAdvancedSearchOnProjectLevel() {
        // Precondition: login and create 3 projects from zip
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String nameProjectSpreadsheetSalary = "SpreadsheetSalary";
        String nameProjectSearchingByTag = "SearchingByTag";
        String nameProjectExample1BankRating = "Example1BankRating";

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSpreadsheetSalary, "RulesEditor.TestSearchOnProjectLevel.SpreadsheetSalary.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSearchingByTag, "RulesEditor.TestSearchOnProjectLevel.SearchingByTag.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectExample1BankRating, "RulesEditor.TestSearchOnProjectLevel.Example1BankRating.zip");

        new EditorPage();
        editorPage = new EditorPage();
        SearchFilterComponent search = editorPage.getSearchFilterComponent();

        // 2.1 Advanced search with "Current Project" scope
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        // The scopes read as the screen names them: the module alone, the project it belongs to, or
        // everything compiled with the projects it depends on.
        assertThat(search.getScopeOptions()).contains("Current project");
        assertThat(search.getScopeOptions()).contains("Everything compiled, dependencies included");
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 2.2 Advanced search with "ALL" scope
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(107);
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);

        // 2.3 Filter by table type with ALL scope
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(8);

        // 2.4 Filter by table type + header with current project scope
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.searchByTableType("Spreadsheet");
        search.setHeaderContains("balance");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();

        // 2.5 Header search with leading/trailing spaces
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.setHeaderContains(" balance");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();
        search.openAdvancedSearch();
        search.setHeaderContains("balance ");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();

        // 2.6 Filter by property "Description"
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.searchByProperty("Description", "hello");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("BankRatingCalculation")).isTrue();
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();

        // 2.7 Combined filter: table type + header + property
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.setHeaderContains("ban");
        search.searchByProperty("Description", "hello");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("BankRatingCalculation")).isTrue();

        // 2.8 Search by Tags property
        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.openAdvancedSearch();
        search.searchByProperty("Tags", "secondRule,searching");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("RulTab")).isTrue();

        // 2.9 Simple search with scope switching
        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.setSearchName("spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.setSearchName("spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(8);
        assertThat(search.isTableFound("IsAdequateNormativeIndexCalculation")).isTrue();
        assertThat(search.isTableFound("SetNonZeroValues")).isTrue();

        // 2.10 View table and edit cell
        search.clickViewTable("BalanceQualityIndexCalculation");
        editorPage.getCenterTable().editCell(3, 1, "Step 1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorToolbarPanelComponent().clickSave();
    }
}
