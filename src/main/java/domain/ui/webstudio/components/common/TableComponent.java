package domain.ui.webstudio.components.common;

import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
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
        WaitUtil.waitForListNotEmpty(() -> rows, 3000, 100, "Waiting for table rows to load before getting cell [" + rowIndex + "," + columnIndex + "]");
        return rows.get(rowIndex - 1 + rowOffset()).getCells().get(columnIndex - 1);
    }

    /**
     * A table written across its columns is drawn with its line numbers in a row above it, which is no row of
     * the table. A table written the usual way round carries them in a column instead, which the row's own
     * cells leave out.
     */
    private int rowOffset() {
        return numberedFirstColumn.getLocator().count() == 0 && firstRowLineNumber.getLocator().count() > 0 ? 1 : 0;
    }

    public String getCellText(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).getInnerText().trim();
    }

    public List<String> getColumn(int columnIndex) {
        WaitUtil.waitForListNotEmpty(() -> rows, 3000, 250, "Waiting for table rows before getting column " + columnIndex);
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
                // A module that keeps recompiling re-renders the table, so the cell never reaches the
                // "stable" state a real dblclick requires; dispatch the event instead.
                getCell(rowIndex, columnIndex).doubleClickWhenSettled();
            }
            editorWrapper.waitForVisible(2000);
            return true;
        }, 10000, 500, "Activating cell editor for cell [" + rowIndex + "," + columnIndex + "]");

        boolean isSelectEditor = inputLocator.getLocator().locator("xpath=self::div[contains(@class,'ant-select')]").count() > 0;
        if (isSelectEditor) {
            // A cell offering a list of values is written by picking from it, not by typing over it.
            pickInSelect(new WebElement(inputLocator, "xpath=.//input", "cellValueInput"), text);
        } else {
            inputLocator.press("Control+A");
            inputLocator.press("Delete");
            inputLocator.fill(text);
            if (pressEnter) {
                inputLocator.press("Enter");
            }
        }
        WaitUtil.sleep(250, "Waiting for cell edit to be applied");
    }

    public void editCell(int rowIndex, int columnIndex, String text) {
        editCell(rowIndex, columnIndex, text, true);
    }

    public List<PlaywrightTableRowComponent> getRows() {
        WaitUtil.waitForCondition(() -> !rows.isEmpty(), 3000, 250, "Waiting for table rows to be loaded");
        return rows.subList(rowOffset(), rows.size());
    }

    public int getRowsCount() {
        WaitUtil.waitForListNotEmpty(() -> rows, 3000, 250, "Waiting for table rows before counting");
        return rows.size() - rowOffset();
    }

    public PlaywrightTableRowComponent getRow(int rowIndex) {
        WaitUtil.waitForListNotEmpty(() -> rows, 3000, 250, "Waiting for table rows before getting row " + rowIndex);
        return rows.get(rowIndex - 1 + rowOffset());
    }

    /**
     * What the cell says about the word it names: the type it stands for and where it comes from, told when
     * the word is pointed at. The word is one of the things the cell names, drawn apart from the rest of the
     * text so it can be pointed at and pressed.
     */
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
        // One box is kept for every hint on the screen, and it holds what was last pointed at until it is
        // told otherwise, so what it says is read once it speaks of the word that was pointed at.
        WaitUtil.requireCondition(() -> hintSaid(hint).contains(variableName), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the hint of '" + variableName + "' to be told");
        return hintSaid(hint).trim();
    }

    /** What the one hint box says, or nothing while it is being drawn again for another word. */
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

    // Inner class for table row operations
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
            // While the table is edited it is drawn with its lines numbered. A table written the usual way
            // round carries the numbers in a column before the first, which is not a cell of the table; a
            // transposed one carries them in a row above it, where they take no column away.
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
