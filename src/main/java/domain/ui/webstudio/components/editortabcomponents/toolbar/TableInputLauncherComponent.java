package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public abstract class TableInputLauncherComponent extends BaseComponent {

    protected static final int PROBE_MS = 2000;

    private final WebElement moduleOnlyCheckbox;
    private final WebElement errorMsg;
    private final WebElement ownLaunchButton;
    private final List<WebElement> otherLaunchButtons;

    protected TableInputLauncherComponent(Page page, String launchTestId) {
        super(page);
        moduleOnlyCheckbox = new WebElement(page, "xpath=//input[@data-testid='launch-module-only']", "moduleOnlyCheckbox");
        errorMsg = new WebElement(page, "xpath=//div[@data-testid='launch-error']", "launchError");
        ownLaunchButton = buttonOf(launchTestId);
        otherLaunchButtons = createElementList("xpath=//button[(@data-testid='run-start' or @data-testid='trace-start'"
                + " or @data-testid='tests-start' or @data-testid='benchmark-start')"
                + " and not(@data-testid='" + launchTestId + "')]", "otherLaunchButtons");
    }

    public void waitForLauncher() {
        ownLaunchButton.waitForVisible(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> otherLaunchButtons.isEmpty(), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the launcher another button of the toolbar opened to close");
    }

    protected void waitForFields() {
        waitForLauncher();
        WaitUtil.requireCondition(() -> !drawnPaths().isEmpty(), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the fields of the table to be drawn");
    }

    public boolean isModuleOnlyChecked() {
        return moduleOnlyCheckbox.isChecked();
    }

    public String getLaunchError() {
        return errorMsg.isVisible(PROBE_MS) ? errorMsg.getText().trim() : "";
    }

    public boolean isAllCasesOffered() {
        return allCasesBox().isVisible(DEFAULT_TIMEOUT_MS);
    }

    public boolean isAllCasesChecked() {
        return allCasesBox().isChecked();
    }

    public void setAllCases(boolean taken) {
        WebElement box = allCasesBox();
        box.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (box.isChecked() != taken) {
            box.click();
        }
    }

    public void pickFirstCase() {
        List<WebElement> boxes = createElementList(
                "xpath=//*[@data-testid='test-cases']//input[@type='checkbox'][not(@data-testid='pick-all-cases')]",
                "caseBoxes");
        WaitUtil.waitForListNotEmpty(() -> boxes, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the cases of the table to be listed");
        boxes.get(0).click();
    }

    public void pickCase(String caseId) {
        WebElement one = new WebElement(page, "xpath=//input[@data-testid='pick-case-" + caseId + "']"
                + " | //*[@data-testid='pick-case-" + caseId + "']//input", "pickCase[" + caseId + "]");
        one.waitForVisible(DEFAULT_TIMEOUT_MS);
        one.click();
    }

    public boolean isCasePicked(String caseId) {
        return new WebElement(page, "xpath=//input[@data-testid='pick-case-" + caseId + "']"
                + " | //*[@data-testid='pick-case-" + caseId + "']//input", "pickCase[" + caseId + "]").isChecked();
    }

    public int getDrawnCaseCount() {
        List<WebElement> boxes = createElementList(
                "xpath=//*[@data-testid='test-cases']//input[@type='checkbox'][not(@data-testid='pick-all-cases')]",
                "caseBoxes");
        WaitUtil.waitForListNotEmpty(() -> boxes, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the cases of the table to be listed");
        return boxes.size();
    }

    public String getTotalCasesText() {
        WebElement total = new WebElement(page,
                "xpath=//*[@data-testid='test-cases']//li[contains(@class,'ant-pagination-total-text')]",
                "totalTestCases");
        return total.isVisible(PROBE_MS) ? total.getText().trim() : "";
    }

    private WebElement allCasesBox() {
        return new WebElement(page, "xpath=//input[@data-testid='pick-all-cases']"
                + " | //*[@data-testid='pick-all-cases']//input", "allCasesBox");
    }

    protected String pathOf(String name) {
        waitForFields();
        if (hasRow(name)) {
            return name;
        }
        return drawnPaths().stream()
                .filter(path -> lastSegmentOf(path).equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The launcher draws no field called '" + name + "': " + drawnPaths()));
    }

    public boolean offersTheFirstElementAsAList() {
        return isFieldChosenFromList(firstElementPath());
    }

    public List<String> getAliasDropdownValues() {
        return fieldOptions(firstElementPath());
    }

    public List<String> getElementsOf(String name) {
        String path = pathOf(name);
        String elementPattern = Pattern.quote(path) + "\\[[^\\[\\]]*\\]";
        Supplier<List<String>> elements = () -> drawnPaths().stream().filter(drawn -> drawn.matches(elementPattern)).toList();
        WaitUtil.isListNotEmpty(elements, PROBE_MS, 200, "Looking for the elements drawn under " + path);
        return elements.get();
    }

    protected String firstElementPath() {
        waitForFields();
        return WaitUtil.waitForResult(() -> drawnPaths().stream().filter(path -> path.endsWith("]")).findFirst(),
                        DEFAULT_TIMEOUT_MS, 200, "Waiting for an element of a collection to be drawn")
                .orElseThrow(() -> new AssertionError("The launcher holds no element of a collection: " + drawnPaths()));
    }

    protected void createFirstUnsetStructure() {
        waitForFields();
        List<WebElement> creators = createElementList(
                "xpath=//button[starts-with(@data-testid,'create-')]", "createButtons");
        if (!creators.isEmpty()) {
            creators.get(0).click();
        }
    }

    protected void expandFirstCollection() {
        waitForFields();
        List<WebElement> switchers = createElementList(
                "xpath=//div[contains(@class,'ant-tree-treenode')][.//button[starts-with(@data-testid,'add-')]]"
                        + "/span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))"
                        + " and not(contains(@class,'ant-tree-switcher_open'))]", "collectionSwitchers");
        if (!switchers.isEmpty()) {
            switchers.get(0).click();
        }
    }

    protected List<String> drawnPaths() {
        return createElementList("xpath=//*[starts-with(@data-testid,'value-') or starts-with(@data-testid,'input-')]",
                "drawnRows").stream()
                .map(element -> element.getAttribute("data-testid"))
                .filter(Objects::nonNull)
                .map(testId -> testId.substring(testId.indexOf('-') + 1))
                .distinct()
                .toList();
    }

    protected List<String> writablePaths() {
        return testIdsStartingWith("edit-");
    }

    private List<String> testIdsStartingWith(String prefix) {
        return createElementList("xpath=//*[starts-with(@data-testid,'" + prefix + "')]", prefix + "rows").stream()
                .map(element -> element.getAttribute("data-testid"))
                .filter(Objects::nonNull)
                .map(testId -> testId.substring(prefix.length()))
                .toList();
    }

    private String lastSegmentOf(String path) {
        int dot = path.lastIndexOf('.');
        int bracket = path.lastIndexOf('[');
        return dot > bracket ? path.substring(dot + 1) : path;
    }

    protected void writeField(String name, String value) {
        String path = pathOf(name);
        openFieldForWriting(path);
        WebElement input = fieldInput(path);
        input.waitForVisible(DEFAULT_TIMEOUT_MS);
        input.fill(value);
        page.keyboard().press("Enter");
        WaitUtil.requireCondition(() -> !input.isVisible(PROBE_MS / 4) || fieldValue(path).contains(value),
                DEFAULT_TIMEOUT_MS, 200, "Waiting for '" + value + "' to be taken as the value of " + path);
    }

    protected void chooseInField(String name, String value) {
        String path = pathOf(name);
        openFieldForWriting(path);
        pickInSelect(selectInputOf(path), value);
    }

    protected List<String> fieldOptions(String name) {
        String path = pathOf(name);
        openFieldForWriting(path);
        WebElement select = selectInputOf(path);
        select.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!"true".equals(select.getAttribute("aria-expanded"))) {
            select.click();
        }
        String listId = WaitUtil.waitForResult(
                        () -> Optional.ofNullable(select.getAttribute("aria-controls")).filter(id -> !id.isBlank()),
                        DEFAULT_TIMEOUT_MS, 200, "Waiting for the list of " + path + " to open")
                .orElseThrow(() -> new AssertionError("The field " + path + " opens no list of values"));
        List<WebElement> options = createElementList(
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "[.//*[@id=\"" + listId + "\"]]"
                        + "//div[contains(@class,'ant-select-item-option')][@title]", "fieldOptions[" + path + "]");
        WaitUtil.waitForListNotEmpty(() -> options, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the values of " + path + " to be listed");
        List<String> values = options.stream().map(WebElement::getText).map(String::trim).toList();
        closeListOf(path);
        return values;
    }

    protected boolean isFieldChosenFromList(String path) {
        openFieldForWriting(path);
        WebElement asList = new WebElement(page,
                "xpath=//div[@data-testid='input-" + path + "'][contains(@class,'ant-select')]", "fieldIsList[" + path + "]");
        return asList.isVisible(PROBE_MS);
    }

    protected void closeListOf(String path) {
        WebElement select = selectInputOf(path);
        if (!"true".equals(select.getAttribute("aria-expanded"))) {
            return;
        }
        String listId = select.getAttribute("aria-controls");
        page.keyboard().press("Escape");
        Locator list = page.locator("xpath=//div[contains(@class,'ant-select-dropdown')][.//*[@id=\"" + listId + "\"]]");
        WaitUtil.requireCondition(() -> !"true".equals(select.getAttribute("aria-expanded")) && !list.first().isVisible(),
                DEFAULT_TIMEOUT_MS, 100, "Waiting for the list of " + path + " to close");
    }

    protected void openFieldForWriting(String path) {
        if (fieldInput(path).isVisible(PROBE_MS / 2)) {
            return;
        }
        WebElement edit = buttonOf("edit-" + path);
        edit.waitForVisible(DEFAULT_TIMEOUT_MS);
        edit.click();
    }

    protected void createStructure(String name) {
        buttonOf("create-" + pathOf(name)).waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    protected void growStructure(String name) {
        buttonOf("add-" + pathOf(name)).waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    protected void toggleRow(String name) {
        String path = pathOf(name);
        Locator switcher = rowOf(path).locator(
                "xpath=./span[contains(@class,'ant-tree-switcher') and not(contains(@class,'ant-tree-switcher-noop'))]");
        WaitUtil.requireCondition(() -> switcher.count() > 0, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the row of " + path + " to offer what it holds");
        switcher.first().click();
    }

    protected boolean hasRow(String path) {
        return rowOf(path).count() > 0;
    }

    protected String fieldValue(String path) {
        WebElement value = new WebElement(page, "xpath=//*[@data-testid='value-" + path + "']", "fieldValue[" + path + "]");
        return value.isVisible(PROBE_MS) ? value.getText().trim() : "";
    }

    private Locator rowOf(String path) {
        return page.locator("xpath=//div[contains(@class,'ant-tree-treenode')]"
                + "[.//*[@data-testid='edit-" + path + "' or @data-testid='create-" + path + "'"
                + " or @data-testid='add-" + path + "' or @data-testid='clear-" + path + "'"
                + " or @data-testid='value-" + path + "' or @data-testid='input-" + path + "']]");
    }

    private WebElement fieldInput(String path) {
        return new WebElement(page, "xpath=(//*[@data-testid='input-" + path + "'][self::input or self::textarea]"
                + " | //*[@data-testid='input-" + path + "']//input"
                + " | //*[@data-testid='input-" + path + "']//textarea)[1]", "fieldInput[" + path + "]");
    }

    private WebElement selectInputOf(String path) {
        return new WebElement(page, "xpath=//div[@data-testid='input-" + path + "']//input", "fieldSelect[" + path + "]");
    }

    protected WebElement buttonOf(String testId) {
        return new WebElement(page, "xpath=//button[@data-testid='" + testId + "']", testId);
    }
}
