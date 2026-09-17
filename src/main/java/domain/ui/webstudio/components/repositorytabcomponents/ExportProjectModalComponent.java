package domain.ui.webstudio.components.repositorytabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.DownloadUtil;
import helpers.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class ExportProjectModalComponent extends BaseComponent {

    private static final Logger LOGGER = LogManager.getLogger(ExportProjectModalComponent.class);

    private static final String MODAL =
            "//div[contains(@class,'ant-modal')][.//*[@data-testid='export-project-revision']]";
    private static final String OPEN_DROPDOWN = "xpath=//div[contains(@class,'ant-select-dropdown')][not(contains(@class,'ant-select-dropdown-hidden'))]";

    private WebElement revisionSelect;
    private WebElement selectedRevisionLabel;
    private WebElement revisionOption;
    private WebElement exportBtn;
    private WebElement cancelBtn;
    private WebElement openDropdown;
    private List<WebElement> revisionOptions;

    public ExportProjectModalComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public ExportProjectModalComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        revisionSelect = new WebElement(page, "xpath=//*[@data-testid='export-project-revision']", "exportRevisionSelect");
        selectedRevisionLabel = new WebElement(page,
                "xpath=//*[@data-testid='export-project-revision']//*[contains(@class,'ant-select-content')]", "exportSelectedRevision");
        revisionOption = new WebElement(page,
                "xpath=//div[contains(@class,'ant-select-item-option')][@title='%s']", "exportRevisionOption");
        revisionOptions = createElementList(
                "xpath=//div[contains(@class,'ant-select-dropdown') and not(contains(@class,'ant-select-dropdown-hidden'))]"
                        + "//div[contains(@class,'ant-select-item-option')]",
                "exportRevisionOptions");
        exportBtn = new WebElement(page, "xpath=//*[@data-testid='export-project-submit']", "exportSubmitBtn");
        cancelBtn = new WebElement(page,
                "xpath=" + MODAL + "//div[contains(@class,'ant-modal-footer')]//button[normalize-space()='Cancel']",
                "exportCancelBtn");
        openDropdown = new WebElement(page, OPEN_DROPDOWN, "exportRevisionDropdown");
    }

    public void waitForDialogToAppear() {
        revisionSelect.waitForVisible(DEFAULT_TIMEOUT_MS);
        exportBtn.waitForVisible(DEFAULT_TIMEOUT_MS);
    }

    public boolean isDialogVisible() {
        return revisionSelect.isVisible(DEFAULT_TIMEOUT_MS / 5);
    }

    public List<String> getAllRevisions() {
        revisionSelect.click();
        List<String> revisions = revisionOptions.stream()
                .map(option -> option.getLocator().getAttribute("title"))
                .filter(title -> title != null && !title.isBlank())
                .toList();
        closeRevisionDropdown();
        return revisions;
    }

    /**
     * Folds the list of revisions away, so what stands under it can be pressed. Escape is not what does it
     * here: the dialog answers Escape by closing itself, and the list is then asked about a dialog that is
     * no longer there. Moving on from the select is what folds the list away.
     */
    private void closeRevisionDropdown() {
        if (!openDropdown.exists()) {
            return;
        }
        page.keyboard().press("Tab");
        if (dropdownClosed()) {
            return;
        }
        LOGGER.warn("The revision list stayed open; pressing the select itself to fold it away");
        revisionSelect.click();
        if (!dropdownClosed()) {
            LOGGER.warn("A select dropdown is still open and may cover the dialog buttons");
        }
    }

    private boolean dropdownClosed() {
        return WaitUtil.waitForCondition(() -> !openDropdown.exists(), DEFAULT_TIMEOUT_MS / 2, 200,
                "Waiting for the revision list to close");
    }

    public String getSelectedRevision() {
        return selectedRevisionLabel.getText().trim();
    }

    public void selectRevision(String revision) {
        LOGGER.info("Selecting revision to export: {}", revision);
        revisionSelect.click();
        revisionOption.format(revision).click();
    }

    public void clickExport() {
        exportBtn.click();
    }

    public File clickExportAndDownload() {
        waitForDialogToAppear();
        File downloadedFile = DownloadUtil.downloadFile(exportBtn.getLocator());
        LOGGER.info("Downloaded file: {} ({} bytes)", downloadedFile.getName(), downloadedFile.length());
        return downloadedFile;
    }

    /**
     * Leaves the export without making it. The list of revisions hangs over the foot of the dialog while it
     * is open, and nothing short of choosing from it folds it away, so the dialog is then left by the cross
     * it carries — which is the same leaving, by the other way the dialog offers.
     */
    public void clickCancel() {
        closeRevisionDropdown();
        if (!isDialogVisible()) {
            return;
        }
        if (openDropdown.exists()) {
            new WebElement(page, "xpath=" + MODAL + "//button[contains(@class,'ant-modal-close')]",
                    "exportDialogClose").click();
        } else {
            cancelBtn.click();
        }
        revisionSelect.waitForHidden(DEFAULT_TIMEOUT_MS);
    }
}
