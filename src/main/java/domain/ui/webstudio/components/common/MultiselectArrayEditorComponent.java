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

    /** Whether the value is among those the cell holds, which the list marks as chosen. */
    public boolean isValueChecked(String value) {
        WebElement option = optionTemplate.format(value);
        option.waitForVisible(DEFAULT_TIMEOUT_MS);
        return String.valueOf(option.getAttribute("class")).contains("ant-select-item-option-selected");
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
            if (!isValueChecked(value)) {
                optionTemplate.format(value).click();
            }
        }
    }

    public void deselectValues(String... values) {
        for (String value : values) {
            if (isValueChecked(value)) {
                optionTemplate.format(value).click();
            }
        }
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
