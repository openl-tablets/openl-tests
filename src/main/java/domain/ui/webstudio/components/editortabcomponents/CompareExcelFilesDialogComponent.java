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
                "xpath=//*[@data-testid='compare-files']//input[@type='file']", "comparedFilesUpload");
        pickedFiles = new WebElement(getPage(), "xpath=//*[@data-testid='compare-file-list']/li", "comparedFiles");
        clearFileBtn = new WebElement(getPage(),
                "xpath=(//*[@data-testid='compare-file-clear'])[1]", "clearComparedFile");
        compareBtn = new WebElement(getPage(), "xpath=//*[@data-testid='compare-start']", "compareFilesBtn");
    }

    /** Whether the window opened on the comparison of two workbooks a reader uploads, which is what it is for. */
    public boolean offersWorkbooksToUpload() {
        return filesToUpload.getLocator().count() > 0;
    }

    public CompareExcelFilesDialogComponent uploadFile(String absoluteFilePath) {
        filesToUpload.setInputFiles(absoluteFilePath);
        WaitUtil.waitForCondition(() -> pickedFiles.getLocator().count() > 0, DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the window to take the workbook");
        return this;
    }

    public int countPickedFiles() {
        return pickedFiles.getLocator().count();
    }

    public CompareExcelFilesDialogComponent clearPickedFiles() {
        while (pickedFiles.getLocator().count() > 0) {
            clearFileBtn.click();
        }
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
