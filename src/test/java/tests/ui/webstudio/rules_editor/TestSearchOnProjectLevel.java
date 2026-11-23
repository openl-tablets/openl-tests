package tests.ui.webstudio.rules_editor;

import configuration.annotations.Description;
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

    private static final int TICKET_TEXT_LENGTH = 33009;
    private static final int SEARCH_TEXT_MAX_LENGTH = 1000;
    private static final String TICKET_SENTENCE = "In show dull give need so held. One order all scale sense her gay style wrote. ";

    @Test
    @TestCaseId({"IPBQA-32590", "EPBDS-14069"})
    @Description("Simple search on project level. EPBDS-14069: a text of 33,009 characters put into the Text in cells "
            + "field of the Extended search of Example 1 - Bank Rating is cut to 1,000 characters and the search "
            + "runs, saying that nothing matched, with no error and no Oops screen.")
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

        String ticketText = TICKET_SENTENCE.repeat(TICKET_TEXT_LENGTH / TICKET_SENTENCE.length() + 1)
                .substring(0, TICKET_TEXT_LENGTH);
        search.closeSearch();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectExample1BankRating);
        search.typeSearchAndEnter(ticketText);
        assertThat(search.getTextInCellsLength())
                .as("The Text in cells field takes no more than 1,000 characters of a text of %d (EPBDS-14069)",
                        TICKET_TEXT_LENGTH)
                .isEqualTo(SEARCH_TEXT_MAX_LENGTH);
        assertThat(search.isOopsShown())
                .as("A very long text in the Extended search must not end in the Oops! Something went wrong screen (EPBDS-14069)")
                .isFalse();
        assertThat(search.getFoundTablesCount()).isZero();
        assertThat(search.getNoResultsMessage())
                .as("The search of a very long text must run and say that nothing matched, not show an Oops screen")
                .isNotEmpty();

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
    @TestCaseId({"IPBQA-32590", "EPBDS-16932", "EPBDS-1656", "EPBDS-5111"})
    @Description("Extended search on project level. EPBDS-16932: a search that names no table type tells how many "
            + "utility tables the type filter hides and Show them lists them. EPBDS-1656: several tags in the Tags "
            + "property find the tables tagged with any of them. EPBDS-5111: a date typed in Text in cells finds "
            + "the tables whose properties hold that day.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testAdvancedSearchOnProjectLevel() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        String nameProjectSpreadsheetSalary = "SpreadsheetSalary";
        String nameProjectSearchingByTag = "SearchingByTag";
        String nameProjectExample1BankRating = "Example1BankRating";
        String nameProjectTechnicalDateTags = "TechnicalDateTags";

        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSpreadsheetSalary, "RulesEditor.TestSearchOnProjectLevel.SpreadsheetSalary.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectSearchingByTag, "RulesEditor.TestSearchOnProjectLevel.SearchingByTag.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectExample1BankRating, "RulesEditor.TestSearchOnProjectLevel.Example1BankRating.zip");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.ZIP_ARCHIVE,
                nameProjectTechnicalDateTags, "RulesEditor.TestSearchOnProjectLevel.TechnicalDateTags.zip");

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
        editorPage.getSaveChangesComponent().clickSave();
        editorPage.waitUntilSpinnerLoaded();

        search.closeSearch();
        editorPage.getEditorToolbarPanelComponent().navigateToProjectsInBreadcrumbs();
        editorPage.getEditorLeftProjectModuleSelectorComponent().selectFirstModule(nameProjectTechnicalDateTags);
        search.setSearchName("Technical");
        search.performSearch();
        assertThat(search.getHiddenByTypeFilterMessage())
                .as("The search must tell that a utility table matched but is hidden by the type filter (EPBDS-16932)")
                .contains("1 table hidden by the type filter");
        assertThat(search.getFoundTablesCount())
                .as("A utility table of the type Other must not be listed while the type filter hides it (EPBDS-16932)")
                .isZero();
        search.clickShowHiddenTables();
        assertThat(search.getFoundTablesCount()).isEqualTo(1);
        assertThat(search.getResultsText())
                .as("Show them must list the technical table in the results (EPBDS-16932)")
                .contains("Technical Notes Of Greetings");

        search.setSearchName("");
        search.setTextInCells("3/12/2014");
        search.performSearch();
        assertThat(search.getFoundTablesCount()).isEqualTo(1);
        assertThat(search.isTableFound("DatedGreeting"))
                .as("A date typed in Text in cells must find the table whose property holds that day (EPBDS-5111)")
                .isTrue();

        search.setTextInCells("");
        search.searchByProperty("Tags", "xx,xxx");
        search.performSearch();
        assertThat(search.getFoundTablesCount()).isEqualTo(2);
        assertThat(search.isTableFound("TaggedFirst"))
                .as("Several tags must find the table tagged with the first of them (EPBDS-1656)")
                .isTrue();
        assertThat(search.isTableFound("TaggedSecond"))
                .as("Several tags must find the table tagged with the second of them (EPBDS-1656)")
                .isTrue();
    }
}
