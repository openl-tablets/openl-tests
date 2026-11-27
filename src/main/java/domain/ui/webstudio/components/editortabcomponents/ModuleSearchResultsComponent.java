package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.common.HeaderSearchComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class ModuleSearchResultsComponent extends BaseComponent {

    private static final String BAND = "xpath=//*[@data-testid='module-found']";
    private static final String ROW = "//li[starts-with(@data-testid,'module-found-')]";
    private static final String SEARCHING_PREFIX = "Searching for";
    private static final String PROJECT_SCOPE_TEXT = "in every cell of project";
    private static final String DEPENDENCIES_SCOPE_TEXT = "and the projects it depends on";
    private static final int PROBE_MS = 1000;
    private static final int SETTLE_MS = 200;
    private static final int COMPILATION_TIMEOUT_MS = 120000;

    private HeaderSearchComponent tablesSearch;
    private WebElement band;
    private WebElement summary;
    private WebElement widerHint;
    private WebElement closeBtn;
    private WebElement nothingFound;
    private WebElement failure;
    private WebElement hiddenNote;
    private WebElement showHiddenLink;
    private List<WebElement> openButtons;
    private WebElement openButtonOf;
    private List<WebElement> groupHeaders;

    public ModuleSearchResultsComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    private void initializeElements() {
        tablesSearch = new HeaderSearchComponent(page, HeaderSearchComponent.Scope.TABLES);
        band = new WebElement(page, BAND, "moduleSearchBand");
        summary = new WebElement(page, "xpath=//*[@data-testid='module-found-summary']", "moduleSearchSummary");
        widerHint = new WebElement(page, "xpath=//*[@data-testid='module-found-wider']", "moduleSearchWiderHint");
        closeBtn = new WebElement(page, "xpath=//button[@data-testid='module-found-close']", "moduleSearchCloseResults");
        nothingFound = new WebElement(page, "xpath=//*[@data-testid='module-found-none']", "moduleSearchNothingFound");
        failure = new WebElement(page, "xpath=//*[@data-testid='module-found-failed']", "moduleSearchFailed");
        hiddenNote = new WebElement(page, "xpath=//*[@data-testid='module-found-hidden']", "moduleSearchHiddenByTypeFilter");
        showHiddenLink = new WebElement(page, "xpath=//*[@data-testid='module-found-hidden']//*[self::a or self::button]",
                "moduleSearchShowHiddenTables");
        openButtons = createElementList("xpath=" + ROW + "//button[starts-with(@data-testid,'module-found-open-')]", "moduleSearchOpenButtons");
        openButtonOf = new WebElement(page, "xpath=" + ROW + "//button[starts-with(@data-testid,'module-found-open-')]"
                + "[contains(concat(' ', normalize-space(.)), ' %1$s (') or contains(concat(' ', normalize-space(.)), ' %1$s(')]",
                "moduleSearchOpenTable");
        groupHeaders = createElementList("xpath=//section[@aria-label][.//li[starts-with(@data-testid,'module-found-')]]",
                "moduleSearchGroups");
    }

    public ModuleSearchResultsComponent searchModule(String text) {
        closeResults();
        tablesSearch.typeLikeUser(text);
        tablesSearch.pressEnter();
        waitForResultsOf(text);
        return this;
    }

    public ModuleSearchResultsComponent searchLongText(String text) {
        closeResults();
        tablesSearch.search(text);
        tablesSearch.pressEnter();
        waitForSettled();
        return this;
    }

    public ModuleSearchResultsComponent searchWholeProject() {
        tablesSearch.pressEnter();
        waitForScope(PROJECT_SCOPE_TEXT);
        return this;
    }

    public ModuleSearchResultsComponent searchDependenciesToo() {
        tablesSearch.pressEnter();
        waitForScope(DEPENDENCIES_SCOPE_TEXT);
        return this;
    }

    public ModuleSearchResultsComponent closeResults() {
        if (closeBtn.isVisible(PROBE_MS)) {
            closeBtn.click();
            WaitUtil.requireCondition(() -> !band.isVisible(PROBE_MS), DEFAULT_TIMEOUT_MS, SETTLE_MS,
                    "Waiting for the search results to be closed");
        }
        return this;
    }

    private void waitForResultsOf(String text) {
        WaitUtil.requireCondition(() -> isSettled() && summary.getText(false).contains("“" + text.trim() + "”"),
                COMPILATION_TIMEOUT_MS, SETTLE_MS, "Waiting for the cells search of '" + text + "' to report its results");
    }

    private void waitForScope(String scopeText) {
        WaitUtil.requireCondition(() -> isSettled() && band.getInnerText().contains(scopeText),
                COMPILATION_TIMEOUT_MS, SETTLE_MS, "Waiting for the search to report its results with scope '" + scopeText + "'");
    }

    private void waitForSettled() {
        WaitUtil.requireCondition(this::isSettled, COMPILATION_TIMEOUT_MS, SETTLE_MS,
                "Waiting for the cells search to report its results");
    }

    private boolean isSettled() {
        if (failure.isVisible(PROBE_MS)) {
            return true;
        }
        if (!summary.isVisible(PROBE_MS) || summary.getText(false).startsWith(SEARCHING_PREFIX)) {
            return false;
        }
        return !openButtons.isEmpty() || nothingFound.isVisible(PROBE_MS);
    }

    public String getSummary() {
        return summary.getText(false).trim();
    }

    public boolean isWiderSearchOffered() {
        return widerHint.isVisible(PROBE_MS);
    }

    public boolean isNothingFoundShown() {
        return nothingFound.isVisible(PROBE_MS);
    }

    public String getNothingFoundMessage() {
        return nothingFound.isVisible(PROBE_MS) ? nothingFound.getInnerText().trim() : "";
    }

    public boolean isFailureShown() {
        return failure.isVisible(PROBE_MS);
    }

    public String getFailureText() {
        return failure.isVisible(PROBE_MS) ? failure.getInnerText().trim() : "";
    }

    public String getHiddenByTypeFilterMessage() {
        return hiddenNote.isVisible(DEFAULT_TIMEOUT_MS) ? hiddenNote.getInnerText().trim() : "";
    }

    public ModuleSearchResultsComponent clickShowHiddenTables() {
        showHiddenLink.click();
        WaitUtil.requireCondition(() -> !hiddenNote.isVisible(PROBE_MS), DEFAULT_TIMEOUT_MS, SETTLE_MS,
                "Waiting for the tables hidden by the type filter to be listed");
        return this;
    }

    public int getFoundTablesCount() {
        return openButtons.size();
    }

    public List<String> getFoundLines() {
        return openButtons.stream().map(button -> button.getInnerText().trim().replaceAll("\\s+", " ")).toList();
    }

    public List<String> getFoundTableNames() {
        return getFoundLines().stream().map(ModuleSearchResultsComponent::tableNameOf).toList();
    }

    public boolean isTableFound(String tableName) {
        return getFoundTableNames().contains(tableName);
    }

    public List<String> getGroupNames() {
        return groupHeaders.stream().map(group -> group.getAttribute("aria-label")).toList();
    }

    public ModuleSearchResultsComponent openTable(String tableName) {
        if (!isTableFound(tableName)) {
            throw new AssertionError("Table '" + tableName + "' is not among the search results: " + getFoundLines());
        }
        openButtonOf.format(tableName).click();
        WebElement compilingScreen = new WebElement(page, "xpath=//div[@data-testid='module-compiling']", "moduleCompilingScreen");
        WebElement namedTable = new WebElement(page,
                "xpath=//table[@data-testid='module-table'][.//td[contains(normalize-space(.),'" + tableName + "')]]",
                "openedTable[" + tableName + "]");
        WaitUtil.requireCondition(() -> compilingScreen.getLocator().count() == 0 && namedTable.isVisible(PROBE_MS),
                COMPILATION_TIMEOUT_MS, 500, "Waiting for the module holding '" + tableName + "' to compile and draw that table");
        return this;
    }

    private static String tableNameOf(String line) {
        int paren = line.indexOf('(');
        String header = paren > 0 ? line.substring(0, paren) : line.replaceFirst("\\s+\\S*[A-Z]+\\d+(:[A-Z]+\\d+)?$", "");
        String[] words = header.trim().split("\\s+");
        return words[words.length - 1].replaceAll("[^a-zA-Z0-9]", "");
    }
}
