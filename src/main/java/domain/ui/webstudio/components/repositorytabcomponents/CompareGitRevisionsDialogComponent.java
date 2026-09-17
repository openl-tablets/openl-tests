package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import helpers.utils.WaitUtil;

/**
 * The comparison of a project against one of its own revisions. It is the same window a comparison of two
 * local versions is shown in — what differs is how the two sides are chosen: the working copy stands on the
 * left, and the revision to hold it against is picked on the right.
 */
public class CompareGitRevisionsDialogComponent extends CompareLocalChangesDialogComponent {

    private static final int PICKER_PROBE_MS = 1000;

    private final WebElement revisionSelect;
    private final WebElement compareBtn;

    public CompareGitRevisionsDialogComponent(Page comparePopup) {
        super(comparePopup);
        revisionSelect = new WebElement(getPage(), "xpath=//div[@data-testid='compare-revision']//input", "revisionSelect");
        compareBtn = new WebElement(getPage(), "xpath=//button[@data-testid='compare-start']", "compareStartBtn");
    }

    /**
     * Picks the revision to hold the working copy against, counted from the newest and from zero: the
     * revision the window opens on is the newest, and 0 asks for that one.
     */
    public void selectRevision(int index) {
        revisionSelect.waitForVisible(DEFAULT_TIMEOUT_MS);
        revisionSelect.click();
        WebElement option = new WebElement(getPage(),
                "xpath=(//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option')][@title])[" + (index + 1) + "]", "revisionOption");
        option.waitForVisible(DEFAULT_TIMEOUT_MS);
        option.click();
    }

    /**
     * The Excel files the working copy offers to be compared, in the order the window offers them; the
     * revision's own list is asked for in the same way.
     */
    public java.util.List<String> getWorkingCopyFilesOffered() {
        return filesOffered("compare-working-file");
    }

    public java.util.List<String> getRevisionFilesOffered() {
        return filesOffered("compare-revision-file");
    }

    private java.util.List<String> filesOffered(String box) {
        WebElement picker = new WebElement(getPage(),
                "xpath=//*[@data-testid='" + box + "']//input", "compareFilePicker");
        picker.waitForVisible(DEFAULT_TIMEOUT_MS);
        picker.click();
        String list = picker.getAttribute("aria-controls");
        String options = "//div[contains(@class,'ant-select-dropdown')][.//*[@id='" + list + "']]"
                + "//div[@role='option']";
        new WebElement(getPage(), "xpath=(" + options + ")[1]", "compareFileOption")
                .waitForVisible(DEFAULT_TIMEOUT_MS);
        java.util.List<String> named = getPage().locator("xpath=" + options)
                .all().stream()
                .map(option -> option.getAttribute("title"))
                .toList();
        picker.press("Escape");
        return named;
    }

    public void clickCompareBtn() {
        compareBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
        compareBtn.click();
        waitForDialogToAppear();
    }

    /**
     * Waits for the window to be ready to be read: either the two sides are already held against each other,
     * or the revision to hold the working copy against is still to be picked.
     */
    @Override
    public CompareGitRevisionsDialogComponent waitForDialogToAppear() {
        WaitUtil.requireCondition(() -> isComparisonDrawn() || compareBtn.isVisible(PICKER_PROBE_MS),
                DEFAULT_TIMEOUT_MS * 2, 250, "Waiting for the comparison of the project to be drawn");
        return this;
    }
}
