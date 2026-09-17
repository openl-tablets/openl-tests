package domain.ui.webstudio.components.common;

import com.microsoft.playwright.PlaywrightException;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

import java.util.List;

public class TableComponent extends BaseComponent {


    private WebElement editorWrapper;
    private WebElement firstRowLineNumber;
    private WebElement numberedFirstColumn;
    private WebElement inputLocator;
    private List<PlaywrightTableRowComponent> rows;
    private WebElement propertyValueTemplate;

    public TableComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public TableComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        editorWrapper = new WebElement(page, "xpath=//*[@data-testid='table-cell-input']", "editorWrapper");
        firstRowLineNumber = createScopedElement("xpath=./tbody/tr[1]//span[@data-testid='table-line-number']", "firstRowLineNumber");
        numberedFirstColumn = createScopedElement("xpath=./tbody/tr[last()]/td[1][.//span[@data-testid='table-line-number']]", "numberedFirstColumn");
        inputLocator = new WebElement(page, "xpath=//*[@data-testid='table-cell-input']", "inputLocator");
        rows = createScopedComponentList(PlaywrightTableRowComponent.class, "xpath=.//tbody/tr", "rowSelectorTemplate");
        propertyValueTemplate = createScopedElement("xpath=//tr/td[text()='%s']/following-sibling::td[1]", "propertyValue");
    }

    public void clickCell(int rowIndex, int columnIndex) {
        getCell(rowIndex, columnIndex).click();
    }

    public WebElement getCell(int rowIndex, int columnIndex) {
        WaitUtil.waitForListNotEmpty(() -> rows, DEFAULT_TIMEOUT_MS, 100, "Waiting for table rows to load before getting cell [" + rowIndex + "," + columnIndex + "]");
        return rows.get(rowIndex - 1 + rowOffset()).getCells().get(columnIndex - 1);
    }

    private int rowOffset() {
        return numberedFirstColumn.getLocator().count() == 0 && firstRowLineNumber.getLocator().count() > 0 ? 1 : 0;
    }

    public String getCellText(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).getInnerText().trim();
    }

    public List<String> getColumn(int columnIndex) {
        WaitUtil.waitForListNotEmpty(() -> rows, DEFAULT_TIMEOUT_MS, 250, "Waiting for table rows before getting column " + columnIndex);
        return rows.stream()
                .skip(rowOffset())
                .map(PlaywrightTableRowComponent::getCells)
                .filter(cells -> cells.size() >= columnIndex)
                .map(cells -> cells.get(columnIndex - 1).getInnerText().trim())
                .toList();
    }

    public void doubleClickCell(int rowIndex, int columnIndex) {
        waitUntilSpinnerLoaded();
        getCell(rowIndex, columnIndex).doubleClick();
    }

    public void editCell(int rowIndex, int columnIndex, String text, boolean pressEnter) {
        waitUntilSpinnerLoaded();
        WaitUtil.retryOnException(() -> {
            try {
                doubleClickCell(rowIndex, columnIndex);
            } catch (RuntimeException neverSettles) {
                getCell(rowIndex, columnIndex).doubleClickWhenSettled();
            }
            editorWrapper.waitForVisible(2000);
            return true;
        }, 30000, 500, "Activating cell editor for cell [" + rowIndex + "," + columnIndex + "]");

        waitUntilTheEditorIsDrawn();
        boolean written = WaitUtil.waitForCondition(() -> {
            if (String.valueOf(inputLocator.getAttribute("class")).contains("ant-select")) {
                WebElement picker = new WebElement(inputLocator, "xpath=.//input", "cellValueInput");
                pickInSelect(picker, text);
                if (pressEnter) {
                    picker.press("Enter");
                }
                return true;
            }
            try {
                inputLocator.press("Control+A");
                inputLocator.press("Delete");
                inputLocator.fill(text);
            } catch (PlaywrightException editorChanged) {
                if (!String.valueOf(editorChanged.getMessage()).contains("Element is not an <input>")) {
                    throw editorChanged;
                }
                return false;
            }
            if (pressEnter) {
                inputLocator.press(keepsWhatIsWritten());
            }
            return true;
        }, 10000, 250, "Writing '" + text + "' into the editor of cell [" + rowIndex + "," + columnIndex + "]");
        if (!written) {
            throw new IllegalStateException("The editor of cell [" + rowIndex + "," + columnIndex
                    + "] took neither the value nor a pick from a list");
        }
        WaitUtil.sleep(250, "Waiting for cell edit to be applied");
    }

    private String keepsWhatIsWritten() {
        return new WebElement(page, "xpath=//textarea[@data-testid='table-cell-input']", "cellLinesEditor").exists()
                ? "Control+Enter"
                : "Enter";
    }

    private void waitUntilTheEditorIsDrawn() {
        String[] last = {null};
        WaitUtil.waitForCondition(() -> {
            String drawn = String.valueOf(inputLocator.getAttribute("class"));
            boolean settled = drawn.equals(last[0]);
            last[0] = drawn;
            return settled;
        }, 5000, 300, "Waiting for the cell editor the cell asks for to be drawn");
    }

    public void editCell(int rowIndex, int columnIndex, String text) {
        editCell(rowIndex, columnIndex, text, true);
    }

    public List<PlaywrightTableRowComponent> getRows() {
        WaitUtil.waitForCondition(() -> !rows.isEmpty(), 3000, 250, "Waiting for table rows to be loaded");
        return rows.subList(rowOffset(), rows.size());
    }

    public int getRowsCount() {
        WaitUtil.waitForListNotEmpty(() -> rows, DEFAULT_TIMEOUT_MS, 250, "Waiting for table rows before counting");
        return rows.size() - rowOffset();
    }

    public PlaywrightTableRowComponent getRow(int rowIndex) {
        WaitUtil.waitForListNotEmpty(() -> rows, DEFAULT_TIMEOUT_MS, 250, "Waiting for table rows before getting row " + rowIndex);
        return rows.get(rowIndex - 1 + rowOffset());
    }

    public String getCellHintText(int rowIndex, int columnIndex, String variableName) {
        WebElement cell = getCell(rowIndex, columnIndex);
        WebElement named = new WebElement(cell,
                String.format("xpath=(.//*[starts-with(@data-testid,'cell-usage-')][contains(normalize-space(.),'%s')])[1]",
                        variableName),
                "cellUsage");
        named.hover();
        WebElement hint = new WebElement(page,
                "xpath=//div[@role='tooltip'][contains(@class,'ant-tooltip-container')]"
                        + "[not(ancestor::div[contains(@class,'ant-tooltip-hidden')])]",
                "cellHint");
        hint.waitForVisible(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> hintSaid(hint).contains(variableName), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the hint of '" + variableName + "' to be told");
        return hintSaid(hint).trim();
    }

    private String hintSaid(WebElement hint) {
        try {
            return hint.getLocator().first().innerText();
        } catch (RuntimeException beingRedrawn) {
            return "";
        }
    }

    public String getPropertyValue(String propertyName) {
        return propertyValueTemplate.format(propertyName).getText().trim();
    }

    public boolean isPropertyPresent(String propertyName) {
        return propertyValueTemplate.format(propertyName).isVisible();
    }

    public List<String> getHeaders() {
        WebElement headerRow = createScopedElement("xpath=.//thead/tr", "headerRow");
        return headerRow.getLocator().locator("xpath=./th").allTextContents();
    }

    public static class PlaywrightTableRowComponent extends BaseComponent {
        List<WebElement> cells;

        public PlaywrightTableRowComponent() {
            super(DriverPool.getPage());
            initializeElements();
        }

        public PlaywrightTableRowComponent(WebElement rootLocator) {
            super(rootLocator);
            initializeElements();
        }

        private void initializeElements() {
            cells = createScopedElementList("xpath=./td[not(position()=1"
                    + " and ancestor::table[1]/tbody/tr[last()]/td[1][.//span[@data-testid='table-line-number']])]", "cells");
        }

        public  List<WebElement> getCells() {
            WaitUtil.waitForListNotEmpty(() -> cells, 250, 50, "Waiting for table row cells to load");
            return cells;
        }

        public List<String> getValue() {
            return cells.stream().map(e -> e.getInnerText().trim()).toList();
        }
    }
}
