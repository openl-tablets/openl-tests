package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import helpers.utils.WaitUtil;

public class ResolveConflictsDialogComponent extends BaseComponent {

    public enum ConflictSide {
        YOURS("your version"),
        THEIRS("their version"),
        BASE("base version");

        private final String label;

        ConflictSide(String label) {
            this.label = label;
        }

        public String downloadLabel() {
            return "Download " + label;
        }

        public String deletedLabel() {
            return "Deleted in " + label;
        }
    }

    private static final String FILE_ROW = "//tr[.//*[normalize-space()='%s']]";

    private WebElement useYoursRadio;
    private WebElement useTheirsRadio;
    private WebElement useBaseRadio;
    private WebElement uploadMergedRadio;
    private WebElement saveButton;
    private WebElement cancelButton;
    private WebElement compareLink;

    public ResolveConflictsDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ResolveConflictsDialogComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        useYoursRadio = createScopedElement("xpath=.//input[@type='radio' and @value='OURS']", "useYoursRadio");
        useTheirsRadio = createScopedElement("xpath=.//input[@type='radio' and @value='THEIRS']", "useTheirsRadio");
        useBaseRadio = createScopedElement("xpath=.//input[@type='radio' and @value='BASE']", "useBaseRadio");
        uploadMergedRadio = createScopedElement("xpath=.//input[@type='radio' and @value='CUSTOM']", "uploadMergedRadio");
        saveButton = createScopedElement("xpath=.//button[contains(@class, 'ant-btn-primary') and contains(., 'Save and Resolve')]", "saveButton");
        cancelButton = createScopedElement("xpath=.//button[contains(@class, 'ant-btn-default') and contains(., 'Cancel')]", "cancelButton");
        compareLink = new WebElement(page, "button:has-text('Compare File Versions')", "compareLink");
    }

    // Availability is reported per file, not per revision, so every lookup is scoped to the row of that file.
    public boolean isDownloadOffered(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//button[normalize-space()='" + side.downloadLabel() + "']").exists();
    }

    public boolean isDeletedStatusShown(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//*[normalize-space()='" + side.deletedLabel() + "']").exists();
    }

    // The deleted side must be a plain label. Rendering it as a button is the defect: clicking it opened a 404.
    public boolean isDeletedStatusRenderedAsButton(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//button[normalize-space()='" + side.deletedLabel() + "']").exists();
    }

    public boolean isCompareOffered(String fileName) {
        return fileRowElement(fileName, "//button[normalize-space()='Compare File Versions']").exists();
    }

    private WebElement fileRowElement(String fileName, String relativeXpath) {
        return new WebElement(page, "xpath=" + String.format(FILE_ROW, fileName) + relativeXpath,
                "conflictRowElement");
    }

    public void waitForDialogToAppear() {
        WaitUtil.waitForCondition(() -> useYoursRadio.isVisible(), 10000, 250, "Waiting for Resolve Conflicts dialog to appear");
    }

    public boolean isDialogVisible() {
        try {
            return useYoursRadio.isVisible(2000);
        } catch (Exception e) {
            return false;
        }
    }

    public void resolveConflictUseYours() {
        WaitUtil.waitForCondition(() -> useYoursRadio.isVisible(), 5000, 100, "Waiting for Use Yours radio button");
        useYoursRadio.click();
        WaitUtil.sleep(500, "Wait for radio button selection");
        clickSave();
    }

    public void resolveConflictUseTheirs() {
        WaitUtil.waitForCondition(() -> useTheirsRadio.isVisible(), 5000, 100, "Waiting for Use Theirs radio button");
        useTheirsRadio.click();
        WaitUtil.sleep(500, "Wait for radio button selection");
        clickSave();
    }

    public void resolveConflictUseBase() {
        WaitUtil.waitForCondition(() -> useBaseRadio.isVisible(), 5000, 100, "Waiting for Use Base radio button");
        useBaseRadio.click();
        WaitUtil.sleep(500, "Wait for radio button selection");
        clickSave();
    }

    public void clickSave() {
        WaitUtil.waitForCondition(() -> saveButton.isVisible() && saveButton.isEnabled(), 5000, 100, "Waiting for Save button");
        saveButton.click();
        WaitUtil.sleep(2000, "Wait for conflict resolution");
    }

    public void clickCancel() {
        cancelButton.click();
        WaitUtil.sleep(1000, "Wait for dialog to close");
    }

    public void clickCompareLink() {
        compareLink.click();
        WaitUtil.sleep(500, "Wait for compare screen to load");
    }

    public CompareLocalChangesDialogComponent clickCompareLinkAsPopup() {
        compareLink.waitForVisible(10000);
        Page popup = page.waitForPopup(() -> {
            compareLink.clickForce();
        });
        popup.waitForLoadState();
        return new CompareLocalChangesDialogComponent(popup);
    }

    // Use for non-Excel (text) file conflicts: Compare opens a nested modal in the same page, not a popup.
    public CompareLocalChangesDialogComponent clickCompareLinkInCurrentPage() {
        compareLink.waitForVisible(10000);
        compareLink.clickForce();
        WaitUtil.sleep(500, "Waiting for text compare modal to open");
        return new CompareLocalChangesDialogComponent(page, true);
    }
}
