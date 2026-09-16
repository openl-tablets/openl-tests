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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 1.2 Search by cell value
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("median");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("step1");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 1.3 Search by table name / signature / return value
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("SalaryCalc");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("Spreadsheet Double SalaryInfo ( String name, Double [] salary )");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("resul");
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();

        // 1.4 Search in another project
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectExample1BankRating);
        search.typeSearchAndEnter("MONEY");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();
        assertThat(search.isTableFound("NetMoneyMarketLiabilitiesScore")).isTrue();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectExample1BankRating);
        search.typeSearchAndEnter(" Balance");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(10);
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();

        // 1.5 Search with trailing space
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectExample1BankRating);
        search.typeSearchAndEnter("Balance ");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(16);
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();

        // 1.6 Search by tag, then view table
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSearchingByTag);
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

        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSearchingByTag);
        search.typeSearchAndEnter("''");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search should say that nothing matched")
                .isNotEmpty();

        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSearchingByTag);
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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        assertThat(search.getScopeOptions()).contains("Current Project");
        assertThat(search.getScopeOptions()).contains("ALL (includes dependency projects)");
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        // 2.2 Advanced search with "ALL" scope
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(8);

        // 2.4 Filter by table type + header with current project scope
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
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
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.setHeaderContains("ban");
        search.searchByProperty("Description", "hello");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("BankRatingCalculation")).isTrue();

        // 2.8 Search by Tags property
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSearchingByTag);
        search.openAdvancedSearch();
        search.searchByProperty("Tags", "secondRule,searching");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("RulTab")).isTrue();

        // 2.9 Simple search with scope switching
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectProject(nameProjectSpreadsheetSalary);
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
