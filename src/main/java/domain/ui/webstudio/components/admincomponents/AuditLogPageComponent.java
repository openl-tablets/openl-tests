package domain.ui.webstudio.components.admincomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.DownloadUtil;
import helpers.utils.WaitUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;

public class AuditLogPageComponent extends BaseComponent {

    private static final long EVENTS_SHOWN_MS = 30000;
    private static final String ROWS = ".//tbody[contains(@class,'ant-table-tbody')]/tr[contains(@class,'ant-table-row')]";
    private static final String ROW = ROWS + "[@data-row-key='%s']";
    private static final String CHANGE_MARK = "^[+~-] ";
    private static final String CELL_TEXTS = "rows => rows.map(row => Array.from(row.cells, cell => cell.innerText))";

    private WebElement firstRow;
    private WebElement clearFiltersBtn;
    private WebElement exportCsvBtn;
    private WebElement objectFilterTrigger;
    private WebElement filterInput;

    public AuditLogPageComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public AuditLogPageComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        firstRow = createScopedElement("xpath=(" + ROWS + ")[1]", "auditFirstRow");
        clearFiltersBtn = createScopedElement("xpath=.//button[normalize-space()='Clear filters']", "auditClearFilters");
        exportCsvBtn = createScopedElement("xpath=.//button[normalize-space()='Export CSV']", "auditExportCsv");
        objectFilterTrigger = createScopedElement("xpath=.//thead//th[.//*[normalize-space()='Object']]"
                + "//span[contains(@class,'ant-table-filter-trigger')]", "auditObjectFilter");
        filterInput = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]"
                + "//div[contains(@class,'ant-table-filter-dropdown')]//input", "auditFilterInput");
    }

    public List<AuditEvent> getEvents() {
        firstRow.waitForVisible(EVENTS_SHOWN_MS);
        Locator rows = firstRow.getLocator().locator("xpath=ancestor::tbody[1]/tr[contains(@class,'ant-table-row')]");
        @SuppressWarnings("unchecked")
        List<List<String>> table = (List<List<String>>) rows.evaluateAll(CELL_TEXTS);
        return table.stream().map(cells -> cells.stream().map(text -> text.replaceAll("\\s+", " ").trim()).toList())
                .map(cells -> new AuditEvent(cells.get(1), cells.get(3), cells.get(4),
                        cells.get(5).replaceFirst(CHANGE_MARK, ""), cells.get(6),
                        cells.get(7), cells.get(8)))
                .toList();
    }

    public List<AuditEvent> waitForEvents(Predicate<List<AuditEvent>> shown) {
        WaitUtil.requireCondition(() -> shown.test(getEvents()), EVENTS_SHOWN_MS, 500,
                "Waiting for the audit log to show the expected events");
        return getEvents();
    }

    public String getDetails(AuditEvent event) {
        Locator row = createScopedElement("xpath=" + String.format(ROW, event.id()), "auditRow").getLocator();
        Locator expand = row.locator("xpath=.//button[contains(@class,'ant-table-row-expand-icon')]");
        if (!"true".equals(expand.getAttribute("aria-expanded"))) {
            expand.click();
        }
        Locator details = row.locator("xpath=following-sibling::tr[1][contains(@class,'ant-table-expanded-row')]");
        details.waitFor();
        return details.innerText().replaceAll("\\s+", " ").trim();
    }

    public AuditLogPageComponent filterByObject(String text) {
        objectFilterTrigger.click();
        filterInput.waitForVisible();
        filterInput.fill(text);
        filterInput.press("Enter");
        return this;
    }

    public AuditLogPageComponent clearFilters() {
        clearFiltersBtn.click();
        return this;
    }

    public String exportCsv() {
        File csv = DownloadUtil.downloadFile(exportCsvBtn.getLocator());
        try {
            return Files.readString(csv.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("The exported audit log could not be read", e);
        } finally {
            DownloadUtil.cleanupDownloadFile(csv);
        }
    }

    public record AuditEvent(String id, String user, String signedInWith, String action, String outcome,
                             String object, String subject) {
    }
}
