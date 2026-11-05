package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

public class TableDependenciesGraphComponent extends BaseComponent {

    private static final String WINDOW = "//div[contains(@class,'ant-modal')][.//*[@data-testid='table-graph']]";
    private static final String USED_BY_LINKS = WINDOW
            + "//strong[normalize-space()='Used by']/ancestor::span[contains(@class,'ant-typography')][1]"
            + "/following-sibling::div[contains(@class,'ant-space')][1]//a";

    private final WebElement title;
    private final WebElement search;
    private final WebElement searchInput;
    private final WebElement optionTemplate;

    public TableDependenciesGraphComponent(Page page) {
        super(page);
        title = new WebElement(page, "xpath=" + WINDOW + "//div[contains(@class,'ant-modal-title')]", "graphTitle");
        search = new WebElement(page, "xpath=" + WINDOW + "//div[@data-testid='table-graph-search']", "graphSearch");
        searchInput = new WebElement(page, "xpath=" + WINDOW + "//div[@data-testid='table-graph-search']//input",
                "graphSearchInput");
        optionTemplate = new WebElement(page,
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option-content') and normalize-space(.)='%s']",
                "graphSearchOption");
    }

    public String getSummary() {
        title.waitForVisible(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> title.getInnerText().contains("dependenc"), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the graph to count its tables and dependencies");
        return title.getInnerText().replaceAll("\\s+", " ").trim();
    }

    public void selectTable(String tableName) {
        search.waitForVisible(DEFAULT_TIMEOUT_MS);
        search.click();
        searchInput.fill(tableName);
        optionTemplate.format(tableName).click();
    }

    public List<String> getUsedBy() {
        Locator links = page.locator("xpath=" + USED_BY_LINKS);
        WaitUtil.waitForCondition(() -> links.count() > 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the tables that use the picked one");
        return links.allInnerTexts().stream().map(String::trim).toList();
    }
}
