package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import helpers.utils.WaitUtil;

import java.nio.file.Paths;

public class CompareExcelFilesDialogComponent extends CompareLocalChangesDialogComponent {

    private WebElement filesToUpload;
    private WebElement pickedFiles;
    private WebElement clearFileBtn;
    private WebElement compareBtn;

    public CompareExcelFilesDialogComponent(Page comparePopup) {
        super(comparePopup);
        filesToUpload = new WebElement(getPage(),
                "xpath=//input[@data-testid='compare-files']", "comparedFilesUpload");
        pickedFiles = new WebElement(getPage(), "xpath=//*[@data-testid='compare-file-list']/li", "comparedFiles");
        clearFileBtn = new WebElement(getPage(),
                "xpath=(//*[@data-testid='compare-file-clear'])[1]", "clearComparedFile");
        compareBtn = new WebElement(getPage(), "xpath=//*[@data-testid='compare-start']", "compareFilesBtn");
    }

    /** Whether the window opened on the comparison of two workbooks a reader uploads, which is what it is for. */
    public boolean offersWorkbooksToUpload() {
        return WaitUtil.waitForCondition(() -> filesToUpload.getLocator().count() > 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the window to offer workbooks to be uploaded");
    }

    public CompareExcelFilesDialogComponent uploadFile(String absoluteFilePath) {
        int taken = pickedFiles.getLocator().count();
        filesToUpload.setInputFiles(absoluteFilePath);
        WaitUtil.requireCondition(() -> pickedFiles.getLocator().count() > taken, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the window to take the workbook");
        return this;
    }

    public int countPickedFiles() {
        return pickedFiles.getLocator().count();
    }

    public CompareExcelFilesDialogComponent clearPickedFiles() {
        for (int picked = pickedFiles.getLocator().count(); picked > 0; picked--) {
            clearFileBtn.click();
        }
        WaitUtil.requireCondition(() -> pickedFiles.getLocator().count() == 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the window to let the workbooks go");
        return this;
    }

    /** Draws the elements that read the same on both sides as well, which the window offers to leave out. */
    public CompareExcelFilesDialogComponent setShowEqualElements(boolean shown) {
        setEqualElementsShown(shown);
        return this;
    }

    public boolean isCompareOffered() {
        return compareBtn.isVisible(PROBE_MS);
    }

    public boolean isCompareEnabled() {
        return compareBtn.isVisible(PROBE_MS) && compareBtn.isEnabled();
    }

    public CompareExcelFilesDialogComponent clickCompareExcel() {
        compareBtn.waitForVisible(DEFAULT_TIMEOUT_MS).click();
        waitForDialogToAppear();
        return this;
    }
}
