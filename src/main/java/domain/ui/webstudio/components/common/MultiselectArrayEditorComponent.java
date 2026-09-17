package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;

/**
 * The list a cell holding several values is written with: the values are ticked in a list drawn under the
 * cell, with a button to take the whole list at once and one to say the choosing is over.
 *
 * <p>The list a cell has open is the one not yet marked as folded away; a list closed a moment ago is left in
 * the page until it has finished folding.
 */
public class MultiselectArrayEditorComponent extends BaseComponent {

    private static final String OPEN_LIST = "//div[contains(@class,'ant-select-dropdown')]"
            + "[not(contains(@class,'ant-select-dropdown-hidden'))]";
    private static final String OPTION = OPEN_LIST + "//div[contains(@class,'ant-select-item-option')]"
            + "[not(contains(@class,'ant-select-item-option-content'))]";
    // Short enough that a button lost to a re-render is retried rather than waited out.
    private static final int ACTION_BUTTON_CLICK_TIMEOUT_MS = DEFAULT_TIMEOUT_MS / 2;
    private static final int PROBE_MS = 2000;

    private WebElement optionTemplate;
    private WebElement actionButtonTemplate;
    private List<WebElement> allOptions;

    public MultiselectArrayEditorComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public MultiselectArrayEditorComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        optionTemplate = new WebElement(page,
                "xpath=(" + OPTION + "[@title=\"%1$s\" or normalize-space(.)=\"%1$s\"])[1]", "multiselectOption");
        actionButtonTemplate = new WebElement(page,
                "xpath=" + OPEN_LIST + "//button[normalize-space()='%s']", "multiselectActionBtn");
        allOptions = createElementList("xpath=" + OPTION, "multiselectOptions");
    }

    public boolean isOpen() {
        return actionButtonTemplate.format("Done").isVisible(PROBE_MS);
    }

    /**
     * Opens the list of the cell being written, which is where its buttons are. The cell keeps the values it
     * holds when the list is folded away, and folds it open again when the cell is pressed.
     */
    private void openList() {
        if (isOpen()) {
            return;
        }
        WebElement cellSelect = new WebElement(page,
                "xpath=//*[@data-testid='table-cell-input'][contains(@class,'ant-select')]//div[contains(@class,'ant-select-content')]",
                "cellMultiselect");
        cellSelect.waitForVisible(DEFAULT_TIMEOUT_MS);
        cellSelect.click();
        WaitUtil.requireCondition(this::isOpen, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the list of the cell to be folded open");
    }

    /** Whether the value is among those the cell holds, which the list marks as chosen. */
    public boolean isValueChecked(String value) {
        WebElement option = narrowedTo(value);
        boolean chosen = String.valueOf(option.getAttribute("class")).contains("ant-select-item-option-selected");
        widenAgain(value);
        return chosen;
    }

    /**
     * Narrows the list to the value and answers the one option left. A long list is drawn a screenful at a
     * time, so a value far down it is not in the page until the list is narrowed — which is what a reader
     * does by typing what they are looking for.
     */
    private WebElement narrowedTo(String value) {
        page.keyboard().type(value);
        WebElement option = optionTemplate.format(value);
        option.waitForVisible(DEFAULT_TIMEOUT_MS);
        return option;
    }

    /** Takes back what was typed to narrow the list, so the next value is looked for in the whole of it. */
    private void widenAgain(String value) {
        for (int typed = 0; typed < value.length(); typed++) {
            page.keyboard().press("Backspace");
        }
    }

    public void verifyChosenValues(List<String> values) {
        for (String value : values) {
            if (!isValueChecked(value)) {
                throw new AssertionError(String.format(
                        "Multiselect value '%s' should be checked but is not", value));
            }
        }
    }

    public void verifyNonChosenValues(String... values) {
        for (String value : values) {
            if (isValueChecked(value)) {
                throw new AssertionError(String.format(
                        "Multiselect value '%s' should NOT be checked but is", value));
            }
        }
    }

    /** Pressing a value that is already chosen would take it away, so only the ones missing are pressed. */
    public void selectValues(String... values) {
        for (String value : values) {
            press(value, false);
        }
    }

    public void deselectValues(String... values) {
        for (String value : values) {
            press(value, true);
        }
    }

    /** Presses the value only when it stands the way it should not, since a press turns it the other way. */
    private void press(String value, boolean chosenNow) {
        WebElement option = narrowedTo(value);
        boolean chosen = String.valueOf(option.getAttribute("class")).contains("ant-select-item-option-selected");
        if (chosen == chosenNow) {
            option.click();
            // Choosing takes back what was typed by itself; taking a value away leaves it standing.
            if (chosenNow) {
                widenAgain(value);
            }
            return;
        }
        widenAgain(value);
    }

    /**
     * Takes the whole list, or lets the whole of it go. The screen offers one button for the two, named
     * after what pressing it would do, so what it is named is what it will do.
     */
    public void setAllValuesChosen(boolean chosen) {
        openList();
        String asked = chosen ? "Select All" : "Deselect All";
        if (!actionButtonTemplate.format(asked).isVisible(PROBE_MS)) {
            throw new AssertionError("The list should offer '" + asked + "' and does not");
        }
        clickActionButton(asked);
    }

    /**
     * Presses one of the editor's buttons (Done / Select All / Deselect All). The list is drawn anew whenever
     * a value is ticked, so the button found a moment ago can be gone by the time it is pressed — the press
     * is retried on the current one instead of waiting the whole timeout out on a detached node.
     */
    public void clickActionButton(String buttonName) {
        WaitUtil.retryOnException(() -> {
            actionButtonTemplate.format(buttonName).click(ACTION_BUTTON_CLICK_TIMEOUT_MS);
            return null;
        }, DEFAULT_TIMEOUT_MS * 2, 500, "Pressing the multiselect button " + buttonName);
    }

    public List<String> getAllValues() {
        return allOptions.stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
    }
}
