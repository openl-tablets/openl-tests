package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import helpers.utils.WaitUtil;

public class CompareGitRevisionsDialogComponent extends CompareLocalChangesDialogComponent {

    private static final int PICKER_PROBE_MS = 1000;

    private final WebElement revisionSelect;
    private final WebElement compareBtn;
    private final WebElement changesSummary;
    private final WebElement changedFileTemplate;

    public CompareGitRevisionsDialogComponent(Page comparePopup) {
        super(comparePopup);
        revisionSelect = new WebElement(getPage(), "xpath=//div[@data-testid='compare-revision']//input", "revisionSelect");
        compareBtn = new WebElement(getPage(), "xpath=//button[@data-testid='compare-start']", "compareStartBtn");
        changesSummary = new WebElement(getPage(), "xpath=//*[@data-testid='compare-revisions-summary']", "compareRevisionsSummary");
        changedFileTemplate = new WebElement(getPage(),
                "xpath=//*[@data-testid='revision-changes-file'][.//strong[normalize-space()=\"%s\"]]", "revisionChangedFile");
    }

    public void selectRevision(int index) {
        revisionSelect.waitForVisible(DEFAULT_TIMEOUT_MS);
        revisionSelect.click();
        WebElement option = new WebElement(getPage(),
                "xpath=(//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option')][@title])[" + (index + 1) + "]", "revisionOption");
        option.waitForVisible(DEFAULT_TIMEOUT_MS);
        option.click();
    }

    public void clickCompareBtn() {
        compareBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        compareBtn.click();
        changesSummary.waitForVisible(COMPARISON_TIMEOUT_MS);
    }

    public CompareGitRevisionsDialogComponent openChangedFile(String fileName) {
        changedFileTemplate.format(fileName).waitForVisible(COMPARISON_TIMEOUT_MS).click();
        WaitUtil.requireCondition(this::isComparisonDrawn, COMPARISON_TIMEOUT_MS, 250,
                "Waiting for '" + fileName + "' to be compared");
        return this;
    }

    @Override
    public CompareGitRevisionsDialogComponent waitForDialogToAppear() {
        WaitUtil.requireCondition(() -> isComparisonDrawn() || compareBtn.isVisible(PICKER_PROBE_MS),
                COMPARISON_TIMEOUT_MS, 250, "Waiting for the comparison of the project to be drawn");
        return this;
    }
}
