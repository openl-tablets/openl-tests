package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.stream.Collectors;

public class SearchFilterComponent extends BaseComponent {

    private static final String FORM = "xpath=//div[@data-testid='table-search-form']";
    private static final String RESULTS = "xpath=//div[@data-testid='table-search-results']";
    private static final int PROBE_MS = 1000;
    private static final int SETTLE_MS = 200;
    private static final int SEARCH_CLICK_TIMEOUT_MS = 3000;
    private static final int COMPILATION_TIMEOUT_MS = 300000;

    private WebElement quickSearch;
    private WebElement openExtendedSearchBtn;
    private WebElement form;
    private WebElement scopeSelect;
    private WebElement kindSelect;
    private WebElement nameInput;
    private WebElement headerInput;
    private WebElement textInput;
    private WebElement propertyAddBtn;
    private WebElement propertyNameTemplate;
    private WebElement propertyValueTemplate;
    private WebElement searchBtn;
    private WebElement closeSearchBtn;
    private WebElement results;
    private List<WebElement> resultRows;
    private List<WebElement> viewTableRows;

    public SearchFilterComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public SearchFilterComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        quickSearch = new WebElement(page, "xpath=//input[@data-testid='module-tables-search']", "quickSearch");
        openExtendedSearchBtn = new WebElement(page, "xpath=//button[@data-testid='module-tables-search-extended']", "openExtendedSearchBtn");
        form = new WebElement(page, FORM, "extendedSearchForm");
        scopeSelect = new WebElement(page, FORM + "//div[@data-testid='table-search-scope']//input", "searchScope");
        kindSelect = new WebElement(page, FORM + "//div[@data-testid='table-search-kind']//input", "searchKind");
        nameInput = new WebElement(page, FORM + "//input[@data-testid='table-search-name']", "searchName");
        headerInput = new WebElement(page, FORM + "//input[@data-testid='table-search-header']", "searchHeader");
        textInput = new WebElement(page, FORM + "//input[@data-testid='table-search-text']", "searchText");
        propertyAddBtn = new WebElement(page, FORM + "//button[@data-testid='table-search-property-add']", "searchPropertyAdd");
        propertyNameTemplate = new WebElement(page, FORM + "//div[@data-testid='table-search-property-%s']//input", "searchPropertyName");
        propertyValueTemplate = new WebElement(page, FORM + "//input[@data-testid='table-search-property-value-%s']", "searchPropertyValue");
        searchBtn = new WebElement(page, "xpath=//button[@data-testid='table-search-run']", "searchBtn");
        closeSearchBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-modal')][.//div[@data-testid='table-search-form']]//button[contains(@class,'ant-modal-close')]", "closeSearchBtn");
        results = new WebElement(page, RESULTS, "searchResults");
        resultRows = createElementList(RESULTS + "//div[starts-with(@data-testid,'table-search-result-')]", "searchResultRows");
        viewTableRows = createElementList(RESULTS + "//div[starts-with(@data-testid,'table-search-result-')]"
                + "//button[starts-with(@data-testid,'table-search-open-')]", "viewTableButtons");
    }

    public SearchFilterComponent typeSearchAndEnter(String text) {
        WaitUtil.retryOnException(() -> {
            openAdvancedSearch();
            textInput.fill(text);
            searchBtn.click(SEARCH_CLICK_TIMEOUT_MS);
            return true;
        }, DEFAULT_TIMEOUT_MS * 2, SETTLE_MS, "Searching for '" + text + "'");
        waitForSearchResult();
        return this;
    }

    public SearchFilterComponent setSearchName(String text) {
        openAdvancedSearch();
        nameInput.fill(text);
        return this;
    }

    public SearchFilterComponent filterTablesRail(String text) {
        quickSearch.click();
        quickSearch.fill(text);
        WaitUtil.sleep(500, "Waiting for the tables rail to be filtered by name");
        return this;
    }

    public SearchFilterComponent openAdvancedSearch() {
        WaitUtil.requireCondition(() -> {
            if (form.isVisible(PROBE_MS)) {
                return true;
            }
            if (openExtendedSearchBtn.isVisible(PROBE_MS)) {
                openExtendedSearchBtn.click();
            }
            return form.isVisible(PROBE_MS);
        }, DEFAULT_TIMEOUT_MS, SETTLE_MS, "Opening the extended search");
        return this;
    }

    public SearchFilterComponent setScope(String scopeValue) {
        openAdvancedSearch();
        pickInSelect(scopeSelect, scopeValue);
        return this;
    }

    public SearchFilterComponent setHeaderContains(String value) {
        openAdvancedSearch();
        headerInput.fill(value);
        return this;
    }

    public SearchFilterComponent setTextInCells(String value) {
        openAdvancedSearch();
        textInput.fill(value);
        return this;
    }

    public SearchFilterComponent searchByTableType(String... types) {
        openAdvancedSearch();
        for (String type : types) {
            pickInSelect(kindSelect, type);
        }
        kindSelect.click();
        return this;
    }

    public SearchFilterComponent searchByProperty(String propertyName, String propertyValue) {
        openAdvancedSearch();
        propertyAddBtn.click();
        int index = Math.max(0, resultPropertyCount() - 1);
        pickInSelect(propertyNameTemplate.format(String.valueOf(index)), propertyName);
        propertyValueTemplate.format(String.valueOf(index)).fill(propertyValue);
        return this;
    }

    private int resultPropertyCount() {
        return createElementList(FORM + "//div[starts-with(@data-testid,'table-search-property-')][not(contains(@data-testid,'value'))]",
                "searchPropertyRows").size();
    }

    public SearchFilterComponent performSearch() {
        searchBtn.click();
        waitForSearchResult();
        return this;
    }

    public SearchFilterComponent waitForSearchResult() {
        WaitUtil.requireCondition(() -> !searchBtn.getAttribute("class").contains("ant-btn-loading"),
                DEFAULT_TIMEOUT_MS, SETTLE_MS, "Waiting for the search to report its results");
        return this;
    }

    public String getNoResultsMessage() {
        WebElement empty = new WebElement(page,
                "xpath=//div[contains(@class,'ant-modal-container')][.//div[@data-testid='table-search-form']]"
                        + "//div[contains(@class,'ant-empty-description')]", "noSearchResults");
        return empty.isVisible(PROBE_MS) ? empty.getText().trim() : "";
    }

    public int getFoundTablesCount() {
        return resultRows.size();
    }

    public boolean isTableFound(String tableName) {
        return getTableNamesInSearchResults().contains(tableName);
    }

    public SearchFilterComponent clickViewTable(String tableName) {
        int index = getTableNamesInSearchResults().indexOf(tableName);
        if (index < 0) {
            throw new AssertionError("Table '" + tableName + "' is not among the search results: "
                    + getTableNamesInSearchResults());
        }
        viewTableRows.get(index).click();
        WebElement compilingScreen = new WebElement(page,
                "xpath=//div[@data-testid='module-compiling']", "moduleCompilingScreen");
        WebElement namedTable = new WebElement(page,
                "xpath=//table[@data-testid='module-table']//tr[1]/td[1][contains(normalize-space(.),'"
                        + tableName + "')]", "openedTable[" + tableName + "]");
        WaitUtil.requireCondition(
                () -> compilingScreen.getLocator().count() == 0 && namedTable.isVisible(PROBE_MS),
                COMPILATION_TIMEOUT_MS, 500,
                "Waiting for the module holding '" + tableName + "' to compile and draw that table");
        return this;
    }

    public void closeSearch() {
        if (closeSearchBtn.isVisible(PROBE_MS)) {
            closeSearchBtn.click();
        }
    }

    public List<String> getTableNamesInSearchResults() {
        return resultRows.stream().map(row -> {
            String header = row.getInnerText().lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.equals("View table") && !line.equals("Show body"))
                    .findFirst()
                    .orElse("");
            int parenIndex = header.indexOf("(");
            String beforeParen = parenIndex > 0 ? header.substring(0, parenIndex).trim() : header;
            String[] parts = beforeParen.split("\\s+");
            return parts.length == 0 ? "" : parts[parts.length - 1].replaceAll("[^a-zA-Z0-9]", "");
        }).collect(Collectors.toList());
    }

    public List<String> getScopeOptions() {
        openAdvancedSearch();
        if (!"true".equals(scopeSelect.getAttribute("aria-expanded"))) {
            scopeSelect.click();
        }
        List<WebElement> options = createElementList(
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option')][@title]", "scopeOptions");
        WaitUtil.waitForListNotEmpty(() -> options, DEFAULT_TIMEOUT_MS, 100, "Waiting for the search scopes to be listed");
        List<String> names = options.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
        scopeSelect.click();
        return names;
    }

}
