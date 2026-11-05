package domain.ui.webstudio.components.common;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import configuration.driver.DriverPool;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Objects;

public class TableComponent extends BaseComponent {

    private static final String DRAWN_ROW = "tbody[not(@aria-hidden='true')]/tr";
    private static final String CELL_USAGE = "[starts-with(@data-testid,'cell-usage-')]";
    private static final String OPEN_DROPDOWN = "//div[contains(concat(' ', normalize-space(@class), ' '), ' ant-dropdown ')]"
            + "[not(contains(@class,'ant-dropdown-hidden'))]";
    private static final String OPEN_CALENDAR = "//div[contains(concat(' ', normalize-space(@class), ' '), ' ant-picker-dropdown ')]"
            + "[not(contains(@class,'ant-picker-dropdown-hidden'))]";
    private static final String OUTLINED_CELL_TEXTS = """
            cells => cells.filter(cell => {
                const style = getComputedStyle(cell);
                const [red, green, blue] = (style.outlineColor.match(/\\d+/g) || []).map(Number);
                return style.outlineStyle === 'solid' && %s;
            }).map(cell => cell.innerText.trim())
            """;
    private static final String RAISED_CELL_TEXTS = OUTLINED_CELL_TEXTS.formatted("red > 2 * green && red > 2 * blue");
    private static final String PICKED_CELL_TEXTS = OUTLINED_CELL_TEXTS.formatted("blue > 2 * red && blue > green");

    private static final String TEXT_INSET_EM = """
            cell => {
                const walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT);
                let node = walker.nextNode();
                while (node && !node.textContent.trim()) {
                    node = walker.nextNode();
                }
                const range = document.createRange();
                range.selectNodeContents(node);
                const text = range.getBoundingClientRect();
                const box = cell.getBoundingClientRect();
                const font = parseFloat(getComputedStyle(cell).fontSize);
                return [(text.left - box.left) / font, (box.right - text.right) / font];
            }
            """;

    private WebElement allCells;
    private WebElement cellEditor;
    private WebElement firstRowLineNumber;
    private WebElement numberedFirstColumn;
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
        allCells = createScopedElement("xpath=.//td[not(ancestor::tbody[@aria-hidden='true'])]", "allCells");
        cellEditor = new WebElement(page, "xpath=//*[@data-testid='table-cell-input']", "cellEditor");
        firstRowLineNumber = createScopedElement("xpath=./tbody/tr[1]//span[@data-testid='table-line-number']", "firstRowLineNumber");
        numberedFirstColumn = createScopedElement("xpath=./tbody/tr[last()]/td[1][.//span[@data-testid='table-line-number']]", "numberedFirstColumn");
        rows = createScopedComponentList(PlaywrightTableRowComponent.class, "xpath=.//" + DRAWN_ROW, "rowSelectorTemplate");
        propertyValueTemplate = createScopedElement("xpath=//tr/td[text()='%s']/following-sibling::td[1]", "propertyValue");
    }

    public void clickCell(int rowIndex, int columnIndex) {
        getCell(rowIndex, columnIndex).click();
    }

    public void scrollToCell(int rowIndex, int columnIndex) {
        getCell(rowIndex, columnIndex).getLocator().scrollIntoViewIfNeeded();
    }

    public double getCellTop(int rowIndex, int columnIndex) {
        return cellBox(rowIndex, columnIndex).y;
    }

    private BoundingBox cellBox(int rowIndex, int columnIndex) {
        return Objects.requireNonNull(getCell(rowIndex, columnIndex).getLocator().first().boundingBox(),
                "Cell [" + rowIndex + "," + columnIndex + "] is not drawn");
    }

    public double getCellHeight(int rowIndex, int columnIndex) {
        return cellBox(rowIndex, columnIndex).height;
    }

    public double getTextInsetFromLeftEm(int rowIndex, int columnIndex) {
        return textInsetsEm(rowIndex, columnIndex).get(0).doubleValue();
    }

    public double getTextInsetFromRightEm(int rowIndex, int columnIndex) {
        return textInsetsEm(rowIndex, columnIndex).get(1).doubleValue();
    }

    @SuppressWarnings("unchecked")
    private List<Number> textInsetsEm(int rowIndex, int columnIndex) {
        return (List<Number>) getCell(rowIndex, columnIndex).getLocator().first().evaluate(TEXT_INSET_EM);
    }

    public WebElement getCell(int rowIndex, int columnIndex) {
        WebElement[] drawn = new WebElement[1];
        WaitUtil.requireCondition(() -> {
            drawn[0] = cellIfDrawn(rowIndex, columnIndex);
            return drawn[0] != null;
        }, DEFAULT_TIMEOUT_MS, 100, "Waiting for cell [" + rowIndex + "," + columnIndex + "] to be drawn");
        return drawn[0];
    }

    private WebElement cellIfDrawn(int rowIndex, int columnIndex) {
        try {
            int wanted = rowIndex - 1 + rowOffset();
            if (rows.size() <= wanted) {
                return null;
            }
            List<WebElement> cells = rows.get(wanted).getCells();
            return cells.size() < columnIndex ? null : cells.get(columnIndex - 1);
        } catch (RuntimeException beingRedrawn) {
            return null;
        }
    }

    private int rowOffset() {
        return numberedFirstColumn.getLocator().count() == 0 && firstRowLineNumber.getLocator().count() > 0 ? 1 : 0;
    }

    public String getCellText(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).getInnerText().trim();
    }

    public List<String> getCellTexts() {
        return allCells.getLocator().allInnerTexts().stream().map(String::trim).toList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRaisedCellTexts() {
        return (List<String>) allCells.getLocator().evaluateAll(RAISED_CELL_TEXTS);
    }

    @SuppressWarnings("unchecked")
    public List<String> getPickedCellTexts() {
        return (List<String>) allCells.getLocator().evaluateAll(PICKED_CELL_TEXTS);
    }

    public void waitForPickedCell(String text) {
        WaitUtil.waitForCondition(() -> List.of(text).equals(getPickedCellTexts()), DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the cell '" + text + "' to be picked");
    }

    public void pressKeyOnPage(String key) {
        page.keyboard().press(key);
    }

    public boolean hasKeyboardFocus() {
        return WaitUtil.waitForCondition(
                () -> (Boolean) getRootLocator().getLocator().first().evaluate("table => document.activeElement === table"),
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the table to hold the keyboard focus");
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

    public WebElement openCellEditor(int rowIndex, int columnIndex) {
        waitUntilSpinnerLoaded();
        WaitUtil.retryOnException(() -> {
            try {
                doubleClickCell(rowIndex, columnIndex);
            } catch (RuntimeException neverSettles) {
                getCell(rowIndex, columnIndex).doubleClickWhenSettled();
            }
            cellEditor.waitForVisible(2000);
            return true;
        }, 30000, 500, "Activating cell editor for cell [" + rowIndex + "," + columnIndex + "]");

        waitUntilTheEditorIsDrawn();
        return cellEditor;
    }

    public void editCell(int rowIndex, int columnIndex, String text, boolean pressEnter) {
        openCellEditor(rowIndex, columnIndex);
        writeIntoOpenEditor(rowIndex, columnIndex, text, pressEnter);
    }

    private void writeIntoOpenEditor(int rowIndex, int columnIndex, String text, boolean pressEnter) {
        boolean written = WaitUtil.waitForCondition(() -> {
            if (String.valueOf(cellEditor.getAttribute("class")).contains("ant-select")) {
                WebElement picker = new WebElement(cellEditor, "xpath=.//input", "cellValueInput");
                pickInSelect(picker, text);
                if (pressEnter) {
                    picker.press("Enter");
                }
                return true;
            }
            try {
                cellEditor.press("Control+A");
                cellEditor.press("Delete");
                cellEditor.fill(text);
            } catch (PlaywrightException editorChanged) {
                if (!String.valueOf(editorChanged.getMessage()).contains("Element is not an <input>")) {
                    throw editorChanged;
                }
                return false;
            }
            if (pressEnter) {
                cellEditor.press(keepsWhatIsWritten());
            }
            return true;
        }, 10000, 250, "Writing '" + text + "' into the editor of cell [" + rowIndex + "," + columnIndex + "]");
        if (!written) {
            throw new IllegalStateException("The editor of cell [" + rowIndex + "," + columnIndex
                    + "] took neither the value nor a pick from a list");
        }
        waitForTheEditToApply();
    }

    private String keepsWhatIsWritten() {
        return new WebElement(page, "xpath=//textarea[@data-testid='table-cell-input']", "cellLinesEditor").exists()
                ? "Control+Enter"
                : "Enter";
    }

    private void waitUntilTheEditorIsDrawn() {
        String[] last = {null};
        WaitUtil.waitForCondition(() -> {
            String drawn = String.valueOf(cellEditor.getAttribute("class"));
            boolean settled = drawn.equals(last[0]);
            last[0] = drawn;
            return settled;
        }, 5000, 300, "Waiting for the cell editor the cell asks for to be drawn");
    }

    public void editCell(int rowIndex, int columnIndex, String text) {
        editCell(rowIndex, columnIndex, text, true);
    }

    public List<String> getEditorsOffered(int rowIndex, int columnIndex) {
        List<String> editors = openEditorSwitch(rowIndex, columnIndex, "").allInnerTexts().stream().map(String::trim).toList();
        page.keyboard().press("Escape");
        page.locator("xpath=" + OPEN_DROPDOWN).first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED).setTimeout(DEFAULT_TIMEOUT_MS));
        if (openEditorInput().isVisible()) {
            openEditorInput().press("Escape");
        }
        return editors;
    }

    public void editCellIn(int rowIndex, int columnIndex, String editor, String text) {
        openEditorSwitch(rowIndex, columnIndex, "[normalize-space()='" + editor + "']").click();
        waitUntilTheEditorIsDrawn();
        writeIntoOpenEditor(rowIndex, columnIndex, text, true);
    }

    private Locator openEditorSwitch(int rowIndex, int columnIndex, String itemPredicate) {
        openCellEditor(rowIndex, columnIndex);
        new WebElement(page, "xpath=//*[@data-testid='table-cell-switch']", "cellEditorSwitch").getLocator()
                .click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
        Locator offered = page.locator("xpath=" + OPEN_DROPDOWN + "//li[@role='menuitem']" + itemPredicate);
        offered.first().waitFor();
        return offered;
    }

    public void markFirstLetters(int letters, String mark) {
        selectFirstLetters(letters);
        clickMarkButton(mark);
    }

    public void clearMarkOfFirstLetters(int letters) {
        selectFirstLetters(letters);
        clickMarkButton("clear");
    }

    private void clickMarkButton(String suffix) {
        new WebElement(page, "xpath=//button[@data-testid='table-edit-mark_" + suffix + "']", "markButton").click();
    }

    private void selectFirstLetters(int letters) {
        Locator input = openEditorInput();
        input.press("Home");
        for (int letter = 0; letter < letters; letter++) {
            input.press("Shift+ArrowRight");
        }
    }

    public List<String> getMarksInEditor() {
        return page.locator("xpath=//*[@data-testid='table-cell-marked-input']//span[@data-mark]").all().stream()
                .map(span -> span.getAttribute("data-mark") + ": " + span.innerText()).toList();
    }

    public String getOpenEditorText() {
        return String.valueOf(openEditorInput().evaluate("element => element.value ?? element.innerText"));
    }

    public void keepEditedText() {
        openEditorInput().press("Enter");
        waitForTheEditToApply();
    }

    public void pickDayInOpenDateEditor(int day) {
        cellEditor.click();
        WebElement dayOfShownMonth = new WebElement(page, "xpath=" + OPEN_CALENDAR
                + "//td[contains(@class,'ant-picker-cell-in-view')][normalize-space()='" + day + "']", "calendarDay");
        dayOfShownMonth.waitForVisible(DEFAULT_TIMEOUT_MS);
        dayOfShownMonth.click();
        waitForTheEditToApply();
    }

    private Locator openEditorInput() {
        return page.locator("xpath=//*[@data-testid='table-cell-input' or @data-testid='table-cell-marked-input']").first();
    }

    public List<String> getBoldParts(int rowIndex, int columnIndex) {
        return boldPartsOf(rowIndex, columnIndex).allInnerTexts().stream().map(String::trim).toList();
    }

    public List<String> getBoldPartColours(int rowIndex, int columnIndex) {
        @SuppressWarnings("unchecked")
        List<String> colours = (List<String>) boldPartsOf(rowIndex, columnIndex)
                .evaluateAll("parts => parts.map(part => getComputedStyle(part).color)");
        return colours;
    }

    private Locator boldPartsOf(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).getLocator().locator("xpath=.//span[contains(@style,'font-weight: bold')]");
    }

    private void waitForTheEditToApply() {
        WaitUtil.sleep(250, "Waiting for cell edit to be applied");
    }

    public int getRowIndexHolding(String... values) {
        List<String> wanted = List.of(values);
        for (int row = 1; row <= getRowsCount(); row++) {
            if (getRow(row).getValue().containsAll(wanted)) {
                return row;
            }
        }
        throw new AssertionError("No row of the table holds " + wanted);
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
        PlaywrightTableRowComponent[] drawn = new PlaywrightTableRowComponent[1];
        WaitUtil.requireCondition(() -> {
            try {
                int wanted = rowIndex - 1 + rowOffset();
                if (rows.size() <= wanted) {
                    return false;
                }
                drawn[0] = rows.get(wanted);
                return true;
            } catch (RuntimeException beingRedrawn) {
                return false;
            }
        }, DEFAULT_TIMEOUT_MS, 100, "Waiting for row " + rowIndex + " to be drawn");
        return drawn[0];
    }

    public String getCellHintText(int rowIndex, int columnIndex, String variableName) {
        WebElement named = cellUsage(rowIndex, columnIndex, "*", String.format("contains(normalize-space(.),'%s')", variableName));
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

    public WebElement getCellLink(int rowIndex, int columnIndex, String linkText) {
        return cellUsage(rowIndex, columnIndex, "button", String.format("normalize-space(.)='%s'", linkText));
    }

    public List<String> getCellLinkTexts(int rowIndex, int columnIndex) {
        return getCell(rowIndex, columnIndex).getLocator()
                .locator("xpath=.//button" + CELL_USAGE)
                .allInnerTexts().stream().map(String::trim).toList();
    }

    private WebElement cellUsage(int rowIndex, int columnIndex, String tag, String textCondition) {
        return new WebElement(getCell(rowIndex, columnIndex),
                String.format("xpath=(.//%s" + CELL_USAGE + "[%s])[1]", tag, textCondition),
                "cellUsage");
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
