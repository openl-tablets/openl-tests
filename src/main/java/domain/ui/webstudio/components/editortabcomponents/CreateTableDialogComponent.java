package domain.ui.webstudio.components.editortabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import configuration.driver.ExecutionMode;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

public class CreateTableDialogComponent extends BaseComponent {

    private static final String MODAL =
            "//div[contains(@class,'ant-modal')][.//div[contains(@class,'ant-modal-title')][contains(normalize-space(.),'Create Table')]]";
    private static final int DATATYPE_TYPE_COLUMN = 0;
    private static final int DATATYPE_NAME_COLUMN = 1;
    private static final int MAX_SKELETON_ROWS = 30;
    private static final int SETTLE_MS = 400;
    private static final int LOADING_TIMEOUT_MS = 60000;

    private WebElement modal;
    private WebElement typeSelect;
    private WebElement typeSelectInput;
    private WebElement nameInput;
    private WebElement moduleInput;
    private WebElement sheetInput;
    private WebElement resultTypeInput;
    private WebElement headerPreview;
    private WebElement createButton;
    private WebElement argumentTypeTemplate;
    private WebElement argumentNameTemplate;
    private WebElement argumentRowTemplate;
    private WebElement insertArgumentTemplate;
    private WebElement cellTemplate;
    private WebElement rowTemplate;
    private WebElement blockedHint;
    private WebElement busyBody;
    private WebElement transposedToggle;
    private int writtenParameterRows;
    private int writtenArgumentRows;

    public CreateTableDialogComponent(WebElement root) {
        super(root);
        initializeElements();
    }

    public CreateTableDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    private void initializeElements() {
        modal = new WebElement(page, "xpath=" + MODAL, "createTableModal");
        busyBody = new WebElement(page,
                "xpath=" + MODAL + "//div[contains(concat(' ',normalize-space(@class),' '),' ant-spin-spinning ')]",
                "createTableBusyBody");
        typeSelect = new WebElement(page, "xpath=//*[@data-testid='create-table-type']", "createTableType");
        typeSelectInput = new WebElement(page,
                "xpath=//*[@data-testid='create-table-type']//input | //input[@data-testid='create-table-type']",
                "createTableTypeInput");
        nameInput = new WebElement(page,
                "xpath=//*[@data-testid='create-table-name']//input | //input[@data-testid='create-table-name']", "createTableName");
        moduleInput = new WebElement(page,
                "xpath=//*[@data-testid='create-table-module']//input | //input[@data-testid='create-table-module']", "createTableModule");
        sheetInput = new WebElement(page,
                "xpath=//*[@data-testid='create-table-sheet']//input | //input[@data-testid='create-table-sheet']", "createTableSheet");
        resultTypeInput = new WebElement(page,
                "xpath=//*[@data-testid='create-table-result-type']//input | //input[@data-testid='create-table-result-type']",
                "createTableResultType");
        headerPreview = new WebElement(page, "xpath=//*[@data-testid='create-table-header']", "createTableHeader");
        createButton = new WebElement(page,
                "xpath=" + MODAL + "//div[contains(@class,'ant-modal-footer')]//button[.//span[normalize-space()='Create']]",
                "createTableSubmit");
        argumentTypeTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-argument-type-%1$s']//input | //input[@data-testid='create-table-argument-type-%1$s']",
                "createTableArgumentType");
        argumentNameTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-argument-name-%1$s']//input | //input[@data-testid='create-table-argument-name-%1$s']",
                "createTableArgumentName");
        argumentRowTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-argument-row-%s']", "createTableArgumentRow");
        insertArgumentTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-argument-row-%s']//button[@aria-label='Insert Argument']",
                "createTableInsertArgument");
        cellTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-cell-%1$s-%2$s']//input | //input[@data-testid='create-table-cell-%1$s-%2$s']",
                "createTableCell");
        rowTemplate = new WebElement(page,
                "xpath=//*[@data-testid='create-table-cell-%s-0']//input | //input[@data-testid='create-table-cell-%s-0']",
                "createTableRow");
        blockedHint = new WebElement(page, "xpath=//*[@data-testid='create-table-blocked']", "createTableBlockedHint");
        transposedToggle = new WebElement(page,
                "xpath=//*[@data-testid='create-table-transposed']//input | //*[@data-testid='create-table-transposed']",
                "createTableTransposed");
    }

    public String getBlockedHint() {
        return blockedHint.waitForVisible(DEFAULT_TIMEOUT_MS).getText().trim();
    }

    public boolean isBlockedHintShown() {
        return blockedHint.isVisible(DEFAULT_TIMEOUT_MS / 5);
    }

    public CreateTableDialogComponent toggleTransposed() {
        waitUntilTheDialogTakesInput();
        transposedToggle.click();
        return this;
    }

    public CreateTableDialogComponent waitForDialogToAppear() {
        createButton.waitForVisible(DEFAULT_TIMEOUT_MS);
        return waitUntilTheDialogTakesInput();
    }

    public CreateTableDialogComponent waitUntilTheDialogTakesInput() {
        if (!WaitUtil.waitForCondition(() -> !busyBody.exists(), LOADING_TIMEOUT_MS, 200,
                "Waiting for the create table dialog to stop loading")) {
            throw new IllegalStateException("The create table dialog kept loading and took no input");
        }
        return this;
    }

    public CreateTableDialogComponent selectType(String type) {
        waitForDialogToAppear();
        writtenParameterRows = 0;
        writtenArgumentRows = 0;
        String option = type.replaceAll("(?i)\\s+table$", "").trim();
        typeSelect.waitForVisible(DEFAULT_TIMEOUT_MS);
        typeSelect.click();
        openDropdownOption(typeSelectInput, option).waitForVisible(DEFAULT_TIMEOUT_MS).click(DEFAULT_TIMEOUT_MS / 2);
        return this;
    }

    public CreateTableDialogComponent clickNext() {
        return waitForDialogToAppear();
    }

    public CreateTableDialogComponent setTechnicalName(String name) {
        nameInput.waitForVisible(DEFAULT_TIMEOUT_MS);
        retype(nameInput, name);
        return this;
    }

    public CreateTableDialogComponent addParameter(String type, String name) {
        int row = writtenParameterRows++;
        if (type != null && !type.isEmpty()) {
            setCell(row, DATATYPE_TYPE_COLUMN, type);
        }
        setCell(row, DATATYPE_NAME_COLUMN, name);
        return this;
    }

    public CreateTableDialogComponent setSimpleRulesInitialParameters(String tableName, String returnValueType) {
        setTechnicalName(tableName);
        if (returnValueType != null && !returnValueType.isEmpty()) {
            resultTypeInput.waitForVisible(DEFAULT_TIMEOUT_MS);
            retypeSuggest(resultTypeInput, returnValueType);
        }
        return this;
    }

    public CreateTableDialogComponent addSimpleRulesParameter(String type, boolean isArray, String name) {
        int row = argumentRow(writtenArgumentRows++);
        String declaredType = isArray && type != null && !type.endsWith("[]") ? type + "[]" : type;
        if (declaredType != null && !declaredType.isEmpty()) {
            retypeSuggest(argumentTypeTemplate.format(String.valueOf(row)), declaredType);
        }
        retype(argumentNameTemplate.format(String.valueOf(row)), name);
        return this;
    }

    public CreateTableDialogComponent setCell(int row, int column, String value) {
        waitUntilTheDialogTakesInput();
        growSkeletonTo(row);
        WebElement cell = cellTemplate.format(String.valueOf(row), String.valueOf(column));
        cell.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (cell.getAttribute("readonly") != null) {
            pickFromList(row, column, cell, value);
        } else if (isSuggest(row, column)) {
            retypeSuggest(cell, value);
        } else {
            retype(cell, value);
        }
        return this;
    }

    private void pickFromList(int row, int column, WebElement cell, String value) {
        WebElement option = openDropdownOption(cell, value);
        cell.click();
        if (!WaitUtil.waitForCondition(() -> option.isVisible(SETTLE_MS), DEFAULT_TIMEOUT_MS / 2, 200,
                "Waiting for the create table cell to offer '" + value + "'")) {
            waitUntilTheDialogTakesInput();
            cell.click();
            if (!WaitUtil.waitForCondition(() -> option.isVisible(SETTLE_MS), DEFAULT_TIMEOUT_MS / 2, 200,
                    "Waiting for the create table cell to offer '" + value + "'")) {
                throw new IllegalStateException("The create table dialog offers no '" + value + "' for this cell");
            }
        }
        option.click(DEFAULT_TIMEOUT_MS / 2);
        WebElement selected = new WebElement(page,
                "xpath=//*[@data-testid='create-table-cell-" + row + "-" + column + "']//*[contains(@class,'ant-select-content')]"
                        + " | //*[@data-testid='create-table-cell-" + row + "-" + column + "']//*[contains(@class,'ant-select-selection-item')]",
                "createTableCellSelection");
        boolean set = WaitUtil.waitForCondition(() -> selected.exists() && value.equals(selected.getText(false)),
                DEFAULT_TIMEOUT_MS / 2, 200, "Waiting for the create table cell to hold '" + value + "'");
        if (!set) {
            throw new IllegalStateException("The create table cell kept '"
                    + (selected.exists() ? selected.getText(false) : "") + "' instead of '" + value + "'");
        }
    }

    private boolean isSuggest(int row, int column) {
        return new WebElement(page,
                "xpath=//*[@data-testid='create-table-cell-" + row + "-" + column + "'][contains(@class,'ant-select')]",
                "createTableSuggestCell").exists();
    }

    public String getCellValue(int row, int column) {
        return cellTemplate.format(String.valueOf(row), String.valueOf(column)).getCurrentInputValue();
    }

    public String getGeneratedHeader() {
        return headerPreview.waitForVisible(DEFAULT_TIMEOUT_MS).getText();
    }

    public CreateTableDialogComponent setCategorySelection(String category) {
        if (category != null && !category.isEmpty()) {
            sheetInput.waitForVisible(DEFAULT_TIMEOUT_MS);
            retypeSuggest(sheetInput, category);
        }
        return this;
    }

    public CreateTableDialogComponent setModule(String moduleName) {
        if (moduleName != null && !moduleName.isEmpty()) {
            moduleInput.waitForVisible(DEFAULT_TIMEOUT_MS);
            retypeSuggest(moduleInput, moduleName);
        }
        return this;
    }

    public boolean isCreateButtonEnabled() {
        return createButton.isEnabled();
    }

    public boolean isDialogVisible() {
        return modal.isVisible(DEFAULT_TIMEOUT_MS / 2);
    }

    public void save() {
        createButton.waitForVisible(DEFAULT_TIMEOUT_MS);
        waitUntilTheDialogTakesInput();
        createButton.click();
        createButton.waitForHidden(DEFAULT_TIMEOUT_MS);
        waitUntilSpinnerLoaded();
    }

    private void growSkeletonTo(int row) {
        for (int current = lastSkeletonRow(); current < row; current++) {
            insertRowBelow(current);
        }
    }

    public CreateTableDialogComponent setRow(int row, String... values) {
        for (int column = 0; column < values.length; column++) {
            if (values[column] != null && !values[column].isEmpty()) {
                setCell(row, column, values[column]);
            }
        }
        return this;
    }

    public CreateTableDialogComponent deleteRow(int row) {
        waitUntilTheDialogTakesInput();
        rowAction(row, "Delete Row").click();
        WaitUtil.waitForCondition(() -> lastSkeletonRow() < row, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the skeleton row to be deleted");
        return this;
    }

    public CreateTableDialogComponent insertRowBelow(int row) {
        waitUntilTheDialogTakesInput();
        int expected = lastSkeletonRow() + 1;
        rowAction(row, "Insert Row Below").click();
        WaitUtil.waitForCondition(() -> lastSkeletonRow() >= expected, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the skeleton to grow a row");
        return waitUntilTheDialogTakesInput();
    }

    private WebElement rowAction(int row, String title) {
        return new WebElement(page,
                "xpath=" + MODAL + "//tr[.//*[@data-testid='create-table-cell-" + row + "-0']]"
                        + "//button[@aria-label='" + title + "']",
                "createTableRowAction").waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    private int lastSkeletonRow() {
        int last = -1;
        for (int row = 0; row < MAX_SKELETON_ROWS; row++) {
            if (!rowTemplate.format(String.valueOf(row), String.valueOf(row)).exists()) {
                break;
            }
            last = row;
        }
        return last;
    }

    private int argumentRow(int row) {
        WebElement name = argumentNameTemplate.format(String.valueOf(row));
        if (!name.exists() && row > 0) {
            waitUntilTheDialogTakesInput();
            insertArgumentTemplate.format(String.valueOf(row - 1)).click();
            name.waitForVisible(DEFAULT_TIMEOUT_MS);
        }
        return row;
    }

    private void retype(WebElement field, String text) {
        boolean accepted = WaitUtil.waitForCondition(() -> {
            waitUntilTheDialogTakesInput();
            field.fill(text);
            WaitUtil.sleep(SETTLE_MS, "Letting the create table dialog fill in what it names itself");
            return text.equals(field.getCurrentInputValue());
        }, DEFAULT_TIMEOUT_MS, 300, "Waiting for the create table field to hold '" + text + "'");
        if (!accepted) {
            throw new IllegalStateException("The create table dialog kept '" + field.getCurrentInputValue()
                    + "' instead of '" + text + "'");
        }
    }

    private void retypeSuggest(WebElement field, String text) {
        boolean accepted = WaitUtil.waitForCondition(() -> {
            waitUntilTheDialogTakesInput();
            clearWithoutLeaving(field);
            field.fillSequentially(text);
            WebElement option = openDropdownOption(field, text);
            if (option.exists()) {
                option.click(DEFAULT_TIMEOUT_MS / 2);
            } else {
                field.press("Enter");
            }
            WaitUtil.sleep(SETTLE_MS, "Letting the create table dialog settle on the picked value");
            return text.equals(field.getCurrentInputValue());
        }, DEFAULT_TIMEOUT_MS, 300, "Waiting for the create table suggest field to hold '" + text + "'");
        if (!accepted) {
            throw new IllegalStateException("The create table dialog kept '" + field.getCurrentInputValue()
                    + "' instead of '" + text + "'");
        }
    }

    /**
     * The value in the list the given field opens. A list closed a moment ago is left in the page until it has
     * finished folding away, and every cell of a column offers the same values, so the list is told from the
     * others by the box it belongs to: a Select names its own list {@code <id of its input>_list}.
     */
    private WebElement openDropdownOption(WebElement field, String text) {
        String owned = field.getAttribute("aria-controls");
        String list = owned != null && !owned.isEmpty() ? owned : field.getAttribute("id") + "_list";
        return new WebElement(page,
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "[.//*[@id='" + list + "']]"
                        + "//div[contains(@class,'ant-select-item-option')][@title='" + text + "']",
                "createTableDropdownOption");
    }

    private void clearWithoutLeaving(WebElement field) {
        field.click();
        boolean dockerMode = ExecutionMode.current() == ExecutionMode.PLAYWRIGHT_DOCKER;
        boolean macHost = System.getProperty("os.name").toLowerCase().contains("mac");
        field.press(!dockerMode && macHost ? "Meta+a" : "Control+a");
        field.press("Backspace");
    }
}
