package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.List;
import java.util.Optional;

public class FailuresOptionComponent extends BaseComponent {

    private static final int PROBE_MS = 1000;

    private final WebElement failuresOnlyCheckbox;
    private final WebElement perTestBox;
    private final WebElement perTestInput;
    private final WebElement perTestValue;

    public FailuresOptionComponent(Page page) {
        this(page, "");
    }

    public FailuresOptionComponent(Page page, String scope) {
        super(page);
        failuresOnlyCheckbox = new WebElement(page, "xpath=" + scope + "//input[@data-testid='tests-failures-only']",
                "failuresOnlyCheckbox");
        perTestBox = new WebElement(page, "xpath=" + scope + "//div[@data-testid='tests-failures']", "failuresPerTestBox");
        perTestInput = new WebElement(page, "xpath=" + scope + "//div[@data-testid='tests-failures']//input",
                "failuresPerTestInput");
        perTestValue = new WebElement(page,
                "xpath=" + scope + "//div[@data-testid='tests-failures']//div[contains(@class,'ant-select-content')]",
                "failuresPerTestValue");
    }

    public FailuresOptionComponent setFailuresOnly(boolean failuresOnly) {
        failuresOnlyCheckbox.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (failuresOnlyCheckbox.isChecked() != failuresOnly) {
            failuresOnlyCheckbox.click();
        }
        WaitUtil.requireCondition(() -> failuresOnlyCheckbox.isChecked() == failuresOnly, DEFAULT_TIMEOUT_MS, 100,
                "Waiting for Failures Only to switch to " + failuresOnly);
        return this;
    }

    public boolean isFailuresPerTestShown() {
        return perTestBox.isVisible(PROBE_MS);
    }

    public boolean isFailuresPerTestEnabled() {
        return isFailuresPerTestShown() && perTestInput.isEnabled();
    }

    public String getFailuresPerTest() {
        perTestBox.waitForVisible(DEFAULT_TIMEOUT_MS);
        return perTestValue.getText().trim();
    }

    public List<String> getFailuresPerTestOptions() {
        perTestInput.waitForVisible(DEFAULT_TIMEOUT_MS);
        if (!"true".equals(perTestInput.getAttribute("aria-expanded"))) {
            perTestInput.click();
        }
        String listId = WaitUtil.waitForResult(
                        () -> Optional.ofNullable(perTestInput.getAttribute("aria-controls")).filter(id -> !id.isBlank()),
                        DEFAULT_TIMEOUT_MS, 200, "Waiting for the list of Failures per test to open")
                .orElseThrow(() -> new AssertionError("Failures per test opens no list of values"));
        List<WebElement> options = createElementList(
                "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "[.//*[@id=\"" + listId + "\"]]//div[contains(@class,'ant-select-item-option')][@title]",
                "failuresPerTestOptions");
        WaitUtil.waitForListNotEmpty(() -> options, DEFAULT_TIMEOUT_MS, 200, "Waiting for the counts of Failures per test to be listed");
        List<String> values = options.stream().map(WebElement::getText).map(String::trim).toList();
        page.keyboard().press("Escape");
        WaitUtil.requireCondition(() -> !"true".equals(perTestInput.getAttribute("aria-expanded")), DEFAULT_TIMEOUT_MS, 100,
                "Waiting for the list of Failures per test to close");
        return values;
    }

    public FailuresOptionComponent pickFailuresPerTest(String count) {
        perTestInput.waitForVisible(DEFAULT_TIMEOUT_MS);
        pickInSelect(perTestInput, count);
        WaitUtil.requireCondition(() -> count.equals(getFailuresPerTest()), DEFAULT_TIMEOUT_MS, 100,
                "Waiting for Failures per test to show " + count);
        return this;
    }
}
