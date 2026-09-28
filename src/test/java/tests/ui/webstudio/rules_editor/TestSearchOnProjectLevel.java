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

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.typeSearchAndEnter("");
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

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
        search.typeSearchAndEnter("Balance");
        search.waitForSearchResult();
        assertThat(search.isTableFound("BalanceDynamicIndexCalculation")).isTrue();
        assertThat(search.isTableFound("BalanceQualityIndexCalculation")).isTrue();

        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSearchingByTag);
        search.typeSearchAndEnter("testingFirst");
        search.waitForSearchResult();
        assertThat(search.isTableFound("RulesName")).isTrue();
        search.clickViewTable("RulesName");
        assertThat(editorPage.getEditorLeftRulesTreeComponent().getSelectedItemText())
                .as("Viewing a table from the results must open that very table")
                .isEqualTo("RulesName");

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

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        assertThat(search.getScopeOptions()).contains("Current project");
        assertThat(search.getScopeOptions()).contains("Everything compiled, dependencies included");
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(62);
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);

        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.searchByTableType("Spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(8);

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

        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectSpreadsheetSalary);
        search.openAdvancedSearch();
        search.setScope("Current project");
        search.setHeaderContains("spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.isTableFound("SalaryCalc")).isTrue();
        assertThat(search.isTableFound("SalaryInfo")).isTrue();

        search.openAdvancedSearch();
        search.setScope("Everything compiled, dependencies included");
        search.setHeaderContains("spreadsheet");
        search.performSearch();
        search.waitForSearchResult();
        assertThat(search.getFoundTablesCount()).isEqualTo(8);
        assertThat(search.isTableFound("IsAdequateNormativeIndexCalculation")).isTrue();
        assertThat(search.isTableFound("SetNonZeroValues")).isTrue();

        search.clickViewTable("BalanceQualityIndexCalculation");
        editorPage.getCenterTable().editCell(3, 1, "Step 1");
        editorPage.getEditorTableActionsPanelComponent().clickSaveChanges();
        editorPage.getEditorToolbarPanelComponent().clickSave();
    }
}
