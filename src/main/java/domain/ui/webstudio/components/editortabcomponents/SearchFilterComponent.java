package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.stream.Collectors;

/** Searching a module's tables: the box above the tables rail, and the extended search it opens. */
public class SearchFilterComponent extends BaseComponent {

    private static final String FORM = "xpath=//div[@data-testid='table-search-form']";
    private static final String RESULTS = "xpath=//div[@data-testid='table-search-results']";
    private static final int PROBE_MS = 1000;
    private static final int SETTLE_MS = 200;
    // Short enough that a search lost with the screen it was opened onto is opened again rather than waited out.
    private static final int SEARCH_CLICK_TIMEOUT_MS = 3000;

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

    /**
     * Searches for the text wherever it is written about a table, which is what the one search box of the
     * old editor did: it matched the text against every cell the table is written in, and the line a table
     * is headed by is one of those cells. That search is the field the extended search calls the text in
     * the cells; the name beside it is a narrower question the old box could not ask.
     */
    public SearchFilterComponent typeSearchAndEnter(String text) {
        // The screen may still be settling on the table just opened, and a search opened onto a screen that
        // is being replaced goes away with it — so what was typed is checked to be still there before the
        // search is run, and the whole of it is done again when it is not.
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

    /** Filters the tables rail itself by name, without opening the extended search. */
    public SearchFilterComponent filterTablesRail(String text) {
        quickSearch.click();
        quickSearch.fill(text);
        WaitUtil.sleep(500, "Waiting for the tables rail to be filtered by name");
        return this;
    }

    /**
     * Opens the extended search. The button stands in the tables rail, which is drawn anew whenever the
     * module screen shows another table — opening one from the results is how the search ends — so a press
     * can land on a rail that is being replaced and open nothing.
     */
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
        // Closing the list by pressing Escape would close the search itself, so the box is closed by
        // pressing it again.
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

    /**
     * Waits for the search that was started to report back. The previous results stay on screen while it
     * runs, so what is waited for is the Search button coming back from its busy state.
     */
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
        return this;
    }

    public void closeSearch() {
        if (closeSearchBtn.isVisible(PROBE_MS)) {
            closeSearchBtn.click();
        }
    }

    /**
     * The names of the tables found, read from the header each result shows: the header names the kind of the
     * table and then the table itself, so the name is the last word before the signature.
     */
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

    /**
     * What the search can be narrowed to. The list is drawn outside the box it hangs under, and the box
     * names an element that holds nothing readable, so the list is read where it is drawn — and only while
     * it stands open, since a list closed before it stays in the page marked as hidden.
     */
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
