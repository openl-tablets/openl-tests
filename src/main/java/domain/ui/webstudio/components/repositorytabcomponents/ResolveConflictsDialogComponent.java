package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareLocalChangesDialogComponent;
import helpers.utils.WaitUtil;

import java.util.List;

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
    private static final String WORKBOOK_TABLE = "//*[@data-testid='workbook-merge-table-%d']";
    private static final long MERGE_EDITOR_SHOWN_MS = 15000;

    private WebElement useYoursRadio;
    private WebElement useTheirsRadio;
    private WebElement useBaseRadio;
    private WebElement uploadMergedRadio;
    private WebElement saveButton;
    private WebElement cancelButton;
    private WebElement compareLink;
    private WebElement textMergeTakeTheirs;
    private WebElement textMergeProgress;
    private WebElement textMergeApply;
    private WebElement workbookMergeApply;

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
        compareLink = new WebElement(page, "xpath=//button[contains(normalize-space(.),'Compare File Versions')]", "compareLink");
        textMergeTakeTheirs = new WebElement(page, "xpath=//button[@data-testid='text-merge-take-theirs']", "textMergeTakeTheirs");
        textMergeProgress = new WebElement(page, "xpath=//*[@data-testid='text-merge-progress']", "textMergeProgress");
        textMergeApply = new WebElement(page, "xpath=//button[@data-testid='text-merge-apply']", "textMergeApply");
        workbookMergeApply = new WebElement(page, "xpath=//button[@data-testid='workbook-merge-apply']", "workbookMergeApply");
    }

    public boolean isDownloadOffered(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//button[normalize-space()='" + side.downloadLabel() + "']").exists();
    }

    public boolean isDeletedStatusShown(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//*[normalize-space()='" + side.deletedLabel() + "']").exists();
    }

    public boolean isDeletedStatusRenderedAsButton(String fileName, ConflictSide side) {
        return fileRowElement(fileName, "//button[normalize-space()='" + side.deletedLabel() + "']").exists();
    }

    public boolean isCompareOffered(String fileName) {
        return compareButton(fileName).exists();
    }

    private WebElement fileRowElement(String fileName, String relativeXpath) {
        return createScopedElement("xpath=." + String.format(FILE_ROW, fileName) + relativeXpath,
                "conflictRowElement");
    }

    public void waitForDialogToAppear() {
        if (!WaitUtil.waitForCondition(() -> useYoursRadio.isVisible(), 10000, 250,
                "Waiting for Resolve Conflicts dialog to appear")) {
            throw new IllegalStateException("The Resolve Conflicts dialog did not open within 10000 ms");
        }
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

    public int getConflictedFilesCount() {
        return theirsRadios().count();
    }

    public int chooseTheirsForEveryFile() {
        Locator radios = theirsRadios();
        int count = radios.count();
        for (int i = 0; i < count; i++) {
            radios.nth(i).check(new Locator.CheckOptions().setForce(true));
        }
        return count;
    }

    private Locator theirsRadios() {
        return page.locator("xpath=//input[@type='radio' and @value='THEIRS']");
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
        return openCompareAsPopup(compareLink);
    }

    private CompareLocalChangesDialogComponent openCompareAsPopup(WebElement compare) {
        compare.waitForVisible(10000);
        Page popup = page.waitForPopup(compare::clickForce);
        popup.waitForLoadState();
        return new CompareLocalChangesDialogComponent(popup);
    }

    public void waitForConflictedFile(String fileName) {
        compareButton(fileName).waitForVisible(MERGE_EDITOR_SHOWN_MS);
    }

    public void chooseResolution(String fileName, String resolution) {
        fileRowElement(fileName, "//label[.//span[normalize-space()='" + resolution + "']]").click();
    }

    public CompareLocalChangesDialogComponent openCompareAsPopup(String fileName) {
        return openCompareAsPopup(compareButton(fileName));
    }

    private WebElement compareButton(String fileName) {
        return fileRowElement(fileName, "//button[normalize-space()='Compare File Versions']");
    }

    public void takeTheirsInTextMerge() {
        textMergeTakeTheirs.waitForVisible(MERGE_EDITOR_SHOWN_MS);
        textMergeTakeTheirs.click();
    }

    public String getTextMergeProgress() {
        return textMergeProgress.getText().trim();
    }

    public String getTextMergeResult(int conflict) {
        return new WebElement(page, "xpath=//*[@data-testid='text-merge-result-" + conflict + "']", "textMergeResult")
                .getInnerText();
    }

    public void applyTextMerge() {
        textMergeApply.click();
        textMergeApply.waitForHidden(10000);
    }

    public List<String> getWorkbookTableActions(int table) {
        Locator takes = page.locator("xpath=" + String.format(WORKBOOK_TABLE, table)
                + "//button[starts-with(@data-testid,'workbook-merge-table-" + table + "-take-')]");
        new WebElement(page, "xpath=//button[@data-testid='workbook-merge-table-" + table + "-take-theirs']",
                "workbookMergeTakeTheirs").waitForVisible(MERGE_EDITOR_SHOWN_MS);
        return takes.all().stream().filter(Locator::isVisible).map(button -> button.innerText().trim()).toList();
    }

    public boolean isWorkbookTableStatusShown(int table, String status) {
        return new WebElement(page, "xpath=" + String.format(WORKBOOK_TABLE, table)
                + "//span[contains(@class,'ant-tag')][normalize-space()='" + status + "']", "workbookMergeTableStatus").isVisible(5000);
    }

    public void takeTheirsForCell(int table, String cell) {
        new WebElement(page, "xpath=//button[@data-testid='workbook-merge-table-" + table + "-" + cell + "-take-theirs']",
                "workbookMergeTakeTheirsForCell").click();
    }

    public String getWorkbookMergedTable(int table) {
        return new WebElement(page, "xpath=//*[@data-testid='workbook-merge-table-" + table + "-merged']",
                "workbookMergedTable").getInnerText();
    }

    public void applyWorkbookMerge() {
        workbookMergeApply.click();
        workbookMergeApply.waitForHidden(10000);
    }
}
