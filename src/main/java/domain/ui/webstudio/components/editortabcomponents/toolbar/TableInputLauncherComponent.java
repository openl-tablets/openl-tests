package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Objects;

/**
 * The panel a table is launched from: the input it is run with, and the buttons that start the run.
 *
 * <p>The panel hangs under the button that opened it and holds the declared parameters as a tree. A plain
 * value is written behind the pencil of its row; a structure is created, grown and folded open by the
 * buttons of its own row. Every row is addressed by the path of the field it stands for, which is the name
 * of the parameter for a row of the first level.
 *
 * <p>Only one launcher stands open at a time and it draws every field it holds into the page, so the fields
 * are read from the page rather than from under the panel itself.
 */
public abstract class TableInputLauncherComponent extends BaseComponent {

    protected static final int PROBE_MS = 2000;

    private final WebElement moduleOnlyCheckbox;
    private final WebElement errorMsg;
    private final WebElement ownLaunchButton;
    private final List<WebElement> otherLaunchButtons;

    /**
     * @param launchTestId the button this launcher starts its own run with, which is what tells this panel
     *                     apart from the one another button of the toolbar opens
     */
    protected TableInputLauncherComponent(Page page, String launchTestId) {
        super(page);
        moduleOnlyCheckbox = new WebElement(page, "xpath=//input[@data-testid='launch-module-only']", "moduleOnlyCheckbox");
        errorMsg = new WebElement(page, "xpath=//div[@data-testid='launch-error']", "launchError");
        ownLaunchButton = buttonOf(launchTestId);
        otherLaunchButtons = createElementList("xpath=//button[(@data-testid='run-start' or @data-testid='trace-start'"
                + " or @data-testid='tests-start' or @data-testid='benchmark-start')"
                + " and not(@data-testid='" + launchTestId + "')]", "otherLaunchButtons");
    }

    /**
     * Waits for the panel to be drawn and for it to be the only one. The button opens the panel at once, but
     * what it asks for comes from the table itself, which is read from the server; and a panel another
     * button of the toolbar left open goes away on the press that opened this one, which the press does not
     * wait for. Until both have happened the fields in the page are not this panel's alone.
     */
    public void waitForLauncher() {
        ownLaunchButton.waitForVisible(DEFAULT_TIMEOUT_MS);
        WaitUtil.requireCondition(() -> otherLaunchButtons.isEmpty(), DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the launcher another button of the toolbar opened to close");
    }

    /** Waits for the fields of the table to be drawn, which the panel does once it has read the table. */
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

    /**
     * The path of the row the given name stands for. A field is addressed by the path it sits at, which
     * for a field of the first level is its name; a field held inside another is named alone all the same,
     * and is then looked up among the rows the launcher draws.
     */
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

    /**
     * The paths of every row the launcher draws, in the order they stand. A row shows its value until it is
     * opened for writing, and shows the box it is written in instead while it is open, so a row is known by
     * whichever of the two it carries.
     */
    protected List<String> drawnPaths() {
        List<String> paths = new java.util.ArrayList<>(testIdsStartingWith("value-"));
        testIdsStartingWith("input-").stream().filter(path -> !paths.contains(path)).forEach(paths::add);
        return paths;
    }

    /** The paths of the fields the launcher offers for writing, in the order they are drawn. */
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

    /** The name a field carries inside whatever holds it: the part of its path after the last step. */
    private String lastSegmentOf(String path) {
        int dot = path.lastIndexOf('.');
        int bracket = path.lastIndexOf('[');
        return dot > bracket ? path.substring(dot + 1) : path;
    }

    /**
     * Writes a value into the field of the given name. The field is opened for writing first: a row shows
     * its value until the pencil beside it is pressed.
     */
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

    /** Picks a value in the field of the given name, which the type of that field offers a list for. */
    protected void chooseInField(String name, String value) {
        String path = pathOf(name);
        openFieldForWriting(path);
        pickInSelect(selectInputOf(path), value);
    }

    /** The values the field of the given name offers, which is the whole of what its type allows. */
    protected List<String> fieldOptions(String name) {
        String path = pathOf(name);
        openFieldForWriting(path);
        WebElement select = selectInputOf(path);
        select.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!"true".equals(select.getAttribute("aria-expanded"))) {
            select.click();
        }
        // The text of an option sits in a box of its own whose class reads as the option's does, so only
        // the option itself — which is the one carrying the value as its title — is counted.
        List<WebElement> options = createElementList(
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option')][@title]", "fieldOptions");
        WaitUtil.waitForListNotEmpty(() -> options, DEFAULT_TIMEOUT_MS, 200,
                "Waiting for the values of " + path + " to be listed");
        return options.stream().map(WebElement::getText).map(String::trim).toList();
    }

    /**
     * Whether the field of the given name is written by choosing from a list. The control is drawn only
     * while the field is open for writing, so the field is opened before it is looked at.
     */
    protected boolean isFieldChosenFromList(String path) {
        openFieldForWriting(path);
        WebElement asList = new WebElement(page,
                "xpath=//div[@data-testid='input-" + path + "'][contains(@class,'ant-select')]", "fieldIsList[" + path + "]");
        return asList.isVisible(PROBE_MS);
    }

    /** A row shows its value until it is opened for writing, and stays open once it is. */
    protected void openFieldForWriting(String path) {
        if (fieldInput(path).isVisible(PROBE_MS / 2)) {
            return;
        }
        WebElement edit = buttonOf("edit-" + path);
        edit.waitForVisible(DEFAULT_TIMEOUT_MS);
        edit.click();
    }

    /** Creates the structure the field stands for, which starts unset and is drawn once it exists. */
    protected void createStructure(String name) {
        buttonOf("create-" + pathOf(name)).waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    /** Adds an element to the list or the map the field stands for. */
    protected void growStructure(String name) {
        buttonOf("add-" + pathOf(name)).waitForVisible(DEFAULT_TIMEOUT_MS).click();
    }

    /** Folds the row of the field open or closed, so what it holds is drawn or put away. */
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

    /**
     * The box a field is written in. A plain field carries the mark itself; a field drawn as a list carries
     * it on the control the box belongs to.
     */
    private WebElement fieldInput(String path) {
        return new WebElement(page, "xpath=(//*[@data-testid='input-" + path + "'][self::input or self::textarea]"
                + " | //*[@data-testid='input-" + path + "']//input"
                + " | //*[@data-testid='input-" + path + "']//textarea)[1]", "fieldInput[" + path + "]");
    }

    /** A field offering a list is a Select, whose value is written into the box the list hangs under. */
    private WebElement selectInputOf(String path) {
        return new WebElement(page, "xpath=//div[@data-testid='input-" + path + "']//input", "fieldSelect[" + path + "]");
    }

    protected WebElement buttonOf(String testId) {
        return new WebElement(page, "xpath=//button[@data-testid='" + testId + "']", testId);
    }
}
