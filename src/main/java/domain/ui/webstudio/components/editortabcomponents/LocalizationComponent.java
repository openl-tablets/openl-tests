package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.StringUtil;
import helpers.utils.WaitUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class LocalizationComponent extends BaseComponent {

    public static final String SWITCHER_ITEM = "Localization";

    private static final String WORKSPACE = "//div[@data-testid='localization-workspace']";
    private static final String TABLE = WORKSPACE + "//*[@data-testid='localization-table']";
    private static final String HEADER_CELLS = TABLE + "//*[contains(@class,'ant-table-thead')]//*[contains(@class,'ant-table-cell')]";
    private static final String OPEN_KEYS = TABLE + "//*[@data-testid='localization-open-key']";
    private static final String ROW_OF_KEY = TABLE + "//div[contains(@class,'ant-table-row')]"
            + "[.//*[@data-testid='localization-open-key'][normalize-space()=%s]]";
    private static final String KEY_PAGE = WORKSPACE + "//*[@data-testid='localization-key']";
    private static final String KEY_PAGE_ROW = KEY_PAGE + "//tr[.//*[@data-testid='localization-key-language']"
            + "[starts-with(normalize-space(.),%1$s)][starts-with(substring-after(normalize-space(.),%1$s),'message')]]";
    private static final String OPEN_DIALOG = "//div[contains(@class,'ant-modal-wrap')][not(contains(@style,'display: none'))]"
            + "//div[contains(@class,'ant-modal')]";
    private static final int POLL_MS = 200;
    private static final int PROBE_MS = 1000;
    private static final int WHEEL_STEP_PX = 250;
    private static final int MAX_SCROLL_STEPS = 200;
    private static final int SCROLL_SETTLE_MS = 150;

    private WebElement workspace;
    private WebElement table;
    private WebElement tableBody;
    private WebElement emptyTable;
    private WebElement keyPage;
    private WebElement missingKeyPage;
    private WebElement ownerBtn;
    private WebElement overrideBtn;
    private WebElement createDefaultBtn;
    private WebElement saveBtn;
    private WebElement undoBtn;
    private WebElement redoBtn;
    private WebElement addLanguageBtn;
    private WebElement removeKeyBtn;
    private WebElement newKeyInput;
    private WebElement newKeyTextInput;
    private WebElement newKeyAddBtn;
    private WebElement cellInput;
    private WebElement inheritBtn;
    private WebElement addKeyNameInput;
    private WebElement addKeyTextInput;
    private WebElement addKeyOkBtn;
    private WebElement addLanguageLanguageInput;
    private WebElement addLanguageCountryInput;
    private WebElement addLanguageOkBtn;
    private WebElement addLanguageCancelBtn;
    private WebElement dialogError;
    private WebElement removeKeyConfirmBtn;

    public LocalizationComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    private void initializeElements() {
        workspace = new WebElement(page, "xpath=" + WORKSPACE, "localizationWorkspace");
        table = new WebElement(page, "xpath=" + TABLE, "localizationTable");
        tableBody = new WebElement(page, "xpath=" + TABLE + "//div[contains(@class,'ant-table-tbody-virtual-holder')]", "localizationTableBody");
        emptyTable = new WebElement(page, "xpath=" + TABLE + "//*[contains(@class,'ant-table-placeholder')]", "localizationEmptyTable");
        keyPage = new WebElement(page, "xpath=" + KEY_PAGE, "localizationKeyPage");
        missingKeyPage = new WebElement(page, "xpath=" + WORKSPACE + "//*[@data-testid='localization-key-missing']", "localizationMissingKey");
        ownerBtn = new WebElement(page, "xpath=//*[@data-testid='localization-key-owner']", "localizationKeyOwnerBtn");
        overrideBtn = new WebElement(page, "xpath=//*[@data-testid='localization-key-add']", "localizationOverrideBtn");
        createDefaultBtn = new WebElement(page, "xpath=//*[@data-testid='localization-create']", "localizationCreateDefaultBtn");
        saveBtn = toolbarButton("localization-save", "localizationSaveBtn");
        undoBtn = toolbarButton("localization-undo", "localizationUndoBtn");
        redoBtn = toolbarButton("localization-redo", "localizationRedoBtn");
        addLanguageBtn = toolbarButton("localization-add-language", "localizationAddLanguageBtn");
        removeKeyBtn = toolbarButton("localization-remove-key", "localizationRemoveKeyBtn");
        newKeyInput = new WebElement(page, "xpath=//*[@data-testid='localization-new-key']", "localizationNewKey");
        newKeyTextInput = new WebElement(page, "xpath=//*[@data-testid='localization-new-key-text']", "localizationNewKeyText");
        newKeyAddBtn = new WebElement(page, "xpath=//*[@data-testid='localization-new-key-add']", "localizationNewKeyAddBtn");
        cellInput = new WebElement(page, "xpath=//textarea[@data-testid='localization-cell-input']", "localizationCellInput");
        inheritBtn = new WebElement(page, "xpath=//*[@data-testid='localization-cell-inherit']", "localizationInheritBtn");
        addKeyNameInput = new WebElement(page, "xpath=//*[@data-testid='localization-add-key-name']", "localizationAddKeyName");
        addKeyTextInput = new WebElement(page, "xpath=//*[@data-testid='localization-add-key-text']", "localizationAddKeyText");
        addKeyOkBtn = new WebElement(page, "xpath=//*[@data-testid='localization-add-key-ok']", "localizationAddKeyOkBtn");
        addLanguageLanguageInput = new WebElement(page, "xpath=(//*[@data-testid='localization-add-language-language']//input"
                + " | //input[@data-testid='localization-add-language-language'])[1]", "localizationAddLanguageLanguage");
        addLanguageCountryInput = new WebElement(page, "xpath=(//*[@data-testid='localization-add-language-country']//input"
                + " | //input[@data-testid='localization-add-language-country'])[1]", "localizationAddLanguageCountry");
        addLanguageOkBtn = new WebElement(page, "xpath=//*[@data-testid='localization-add-language-ok']", "localizationAddLanguageOkBtn");
        addLanguageCancelBtn = new WebElement(page, "xpath=" + OPEN_DIALOG + "//button[normalize-space()='Cancel']", "localizationAddLanguageCancelBtn");
        dialogError = new WebElement(page, "xpath=" + OPEN_DIALOG + "//div[contains(@class,'ant-form-item-explain-error')]", "localizationDialogError");
        removeKeyConfirmBtn = new WebElement(page, "xpath=" + OPEN_DIALOG + "//button[normalize-space()='Remove Key']", "localizationRemoveKeyConfirmBtn");
    }

    private WebElement toolbarButton(String testId, String name) {
        return new WebElement(page, "xpath=//*[@data-testid='localization-toolbar']//*[@data-testid='" + testId + "']", name);
    }

    public LocalizationComponent waitForOpened() {
        WaitUtil.requireCondition(() -> page.url().contains("/localization"), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the Localization screen to open");
        workspace.waitForVisible(DEFAULT_TIMEOUT_MS * 3);
        return this;
    }

    public LocalizationComponent waitForTable() {
        table.waitForVisible(DEFAULT_TIMEOUT_MS * 3);
        WaitUtil.requireCondition(() -> !drawnKeys().isEmpty() || emptyTable.exists(), DEFAULT_TIMEOUT_MS * 3, POLL_MS,
                "Waiting for the table to draw its keys or tell it has none");
        return this;
    }

    public List<String> getColumnTitles() {
        return headerLines(0);
    }

    public List<String> getColumnFiles() {
        return headerLines(1);
    }

    public List<String> getKeys() {
        waitForTable();
        Set<String> keys = new LinkedHashSet<>();
        scrollThroughRows(() -> {
            keys.addAll(drawnKeys());
            return false;
        });
        return List.copyOf(keys);
    }

    public boolean waitForKeys(Predicate<List<String>> expected) {
        return WaitUtil.waitForCondition(() -> expected.test(getKeys()), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the table to list the expected keys");
    }

    public String getCellText(String key, String language) {
        WebElement cell = cellOf(key, language);
        String[] read = {null};
        WaitUtil.requireCondition(() -> {
            String now = normalize(cell.getLocator().innerText());
            boolean settled = now.equals(read[0]);
            read[0] = now;
            return settled;
        }, DEFAULT_TIMEOUT_MS, POLL_MS, "Waiting for the cell of '" + key + "' in " + language + " to settle");
        return read[0];
    }

    public LocalizationComponent editCell(String key, String language, String text) {
        openCellEditor(key, language);
        cellInput.fill(text);
        cellInput.press("Enter");
        cellInput.waitForHidden(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public LocalizationComponent useInheritedText(String key, String language) {
        openCellEditor(key, language);
        inheritBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        cellInput.waitForHidden(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public LocalizationComponent addKeyInNewRow(String key, String defaultText) {
        newKeyInput.waitForVisible(DEFAULT_TIMEOUT_MS).fill(key);
        newKeyTextInput.fill(defaultText);
        newKeyAddBtn.click();
        revealRow(key);
        return this;
    }

    public LocalizationComponent openKey(String key) {
        revealRow(key).child("xpath=.//*[@data-testid='localization-open-key']").click();
        keyPage.waitForVisible(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public LocalizationComponent showAllMessages() {
        new WebElement(page, "xpath=//*[@data-testid='localization-rail']//*[normalize-space()='All Messages'][not(*[normalize-space()='All Messages'])]",
                "allMessagesNode").click();
        return waitForTable();
    }

    public LocalizationComponent removeKey(String key) {
        openKey(key);
        removeKeyBtn.click();
        removeKeyConfirmBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        removeKeyConfirmBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        waitForTable();
        WaitUtil.requireCondition(() -> !new WebElement(page, "xpath=" + rowOf(key), "removedRow").exists(), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the key '" + key + "' to leave the table");
        return this;
    }

    public String getKeyPageText(String language) {
        keyPage.waitForVisible(DEFAULT_TIMEOUT_MS);
        return normalize(keyPageRow(language, "//td").getInnerText());
    }

    public String getKeyPageLanguageBackground(String language) {
        keyPage.waitForVisible(DEFAULT_TIMEOUT_MS);
        return keyPageRow(language, "//th").getCssValue("background-color");
    }

    public String getMissingKeyText() {
        return normalize(missingKeyPage.getInnerText());
    }

    public String getOwnerButtonText() {
        return ownerBtn.waitForVisible(DEFAULT_TIMEOUT_MS).getText().trim();
    }

    public void openInOwnerProject() {
        ownerBtn.click();
    }

    public String overrideHere(String defaultText) {
        overrideBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        String key = addKeyNameInput.waitForVisible(DEFAULT_TIMEOUT_MS).getCurrentInputValue();
        addKeyTextInput.fill(defaultText);
        addKeyOkBtn.click();
        addKeyOkBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        return key;
    }

    public LocalizationComponent createDefaultLanguage() {
        createDefaultBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        return waitForTable();
    }

    public LocalizationComponent undo() {
        undoBtn.click();
        return this;
    }

    public LocalizationComponent redo() {
        redoBtn.click();
        return this;
    }

    public LocalizationComponent save() {
        saveBtn.click();
        WaitUtil.requireCondition(() -> !saveBtn.isEnabled() && !undoBtn.isEnabled(), DEFAULT_TIMEOUT_MS * 2, POLL_MS,
                "Waiting for the messages to be saved, which clears the steps to undo");
        return this;
    }

    public LocalizationComponent addLanguage(String language, String country) {
        openAddLanguageDialog(language, country);
        addLanguageOkBtn.click();
        addLanguageOkBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
        return this;
    }

    public String addLanguageExpectingRefusal(String language) {
        openAddLanguageDialog(language, null);
        addLanguageOkBtn.click();
        String refusal = dialogError.waitForVisible(DEFAULT_TIMEOUT_MS).getText().trim();
        addLanguageCancelBtn.click();
        return refusal;
    }

    private void openAddLanguageDialog(String language, String country) {
        addLanguageBtn.click();
        addLanguageOkBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        pickInSelect(addLanguageLanguageInput, language);
        if (country != null) {
            pickInSelect(addLanguageCountryInput, country);
        }
    }

    private void openCellEditor(String key, String language) {
        cellOf(key, language).child("xpath=.//*[@data-testid='localization-cell']").click();
        cellInput.waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    private WebElement cellOf(String key, String language) {
        int column = columnIndex(language);
        revealRow(key);
        return new WebElement(page, "xpath=(" + rowOf(key) + "/div[contains(@class,'ant-table-cell')])[" + (column + 1) + "]",
                "localizationCell[" + key + "," + language + "]");
    }

    private WebElement revealRow(String key) {
        waitForTable();
        WebElement row = new WebElement(page, "xpath=" + rowOf(key), "localizationRow[" + key + "]");
        if (!row.exists()) {
            scrollThroughRows(row::exists);
        }
        WaitUtil.requireCondition(() -> row.isVisible(PROBE_MS), DEFAULT_TIMEOUT_MS, POLL_MS,
                "Waiting for the row of the key '" + key + "'; the table draws: " + drawnKeys());
        return row;
    }

    private void scrollThroughRows(BooleanSupplier found) {
        if (!isTableScrollable()) {
            found.getAsBoolean();
            return;
        }
        scrollTableBy(-WHEEL_STEP_PX * MAX_SCROLL_STEPS);
        for (int step = 0; step < MAX_SCROLL_STEPS && !found.getAsBoolean() && !isTableScrolledToEnd(); step++) {
            scrollTableBy(WHEEL_STEP_PX);
        }
        found.getAsBoolean();
    }

    private boolean isTableScrollable() {
        return tableBody.exists() && Boolean.TRUE.equals(tableBody.getLocator().first()
                .evaluate("body => body.scrollHeight > body.clientHeight"));
    }

    private boolean isTableScrolledToEnd() {
        return Boolean.TRUE.equals(tableBody.getLocator().first()
                .evaluate("body => body.scrollTop + body.clientHeight >= body.scrollHeight - 1"));
    }

    private void scrollTableBy(int pixels) {
        table.hover();
        page.mouse().wheel(0, pixels);
        WaitUtil.sleep(SCROLL_SETTLE_MS, "Waiting for the table to draw the rows scrolled into view");
    }

    private List<String> drawnKeys() {
        return page.locator("xpath=" + OPEN_KEYS).allInnerTexts().stream().map(String::trim).toList();
    }

    private static String rowOf(String key) {
        return ROW_OF_KEY.formatted(StringUtil.xpathLiteral(key));
    }

    private WebElement keyPageRow(String language, String part) {
        return new WebElement(page, "xpath=(" + KEY_PAGE_ROW.formatted(StringUtil.xpathLiteral(language)) + part + ")[1]",
                "localizationKeyPageRow[" + language + "]");
    }

    private int columnIndex(String language) {
        waitForTable();
        List<String> titles = headerCells().stream().map(text -> line(text, 0)).toList();
        int index = titles.indexOf(language);
        if (index < 0) {
            throw new AssertionError("The table has no column '" + language + "'; its columns: " + titles);
        }
        return index;
    }

    private List<String> headerLines(int line) {
        waitForTable();
        return headerCells().stream().skip(1).map(text -> line(text, line)).filter(text -> !text.isEmpty()).toList();
    }

    private List<String> headerCells() {
        return page.locator("xpath=" + HEADER_CELLS).allInnerTexts();
    }

    private static String line(String text, int index) {
        return text.strip().lines().skip(index).findFirst().orElse("").trim();
    }

    private static String normalize(String text) {
        return text.strip().lines().map(String::trim).filter(line -> !line.isEmpty())
                .reduce((first, second) -> first + " | " + second).orElse("");
    }
}
