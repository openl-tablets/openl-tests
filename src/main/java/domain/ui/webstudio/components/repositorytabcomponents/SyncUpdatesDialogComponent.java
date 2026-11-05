package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

import java.util.List;

public class SyncUpdatesDialogComponent extends BaseComponent {

    private static final String MODAL_ROOT =
            "//div[contains(@class,'ant-modal')][.//div[contains(@class,'ant-modal-title') and contains(normalize-space(),'Sync updates')]]";

    private static final String SCOPE_PROJECT = MODAL_ROOT + "//div[@data-testid='merge-scope-project']";
    private static final String CHANGED_IN_BOTH = SCOPE_PROJECT + "//*[@data-testid='merge-scope-changed-in-both-file']";
    private static final long SCOPE_SHOWN_MS = 15000;

    private WebElement modalTitle;
    private WebElement receiveBtn;
    private WebElement sendBtn;
    private WebElement closeBtn;

    public SyncUpdatesDialogComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    private void initializeElements() {
        modalTitle = new WebElement(DriverPool.getPage(),
                "xpath=" + MODAL_ROOT + "//div[contains(@class,'ant-modal-title')]", "syncUpdatesTitle");
        receiveBtn = new WebElement(DriverPool.getPage(),
                "xpath=" + MODAL_ROOT + "//button[.//span[normalize-space()='Receive their updates'] or normalize-space()='Receive their updates']", "syncReceiveBtn");
        sendBtn = new WebElement(DriverPool.getPage(),
                "xpath=" + MODAL_ROOT + "//button[.//span[normalize-space()='Send your updates'] or normalize-space()='Send your updates']", "syncSendBtn");
        closeBtn = new WebElement(DriverPool.getPage(),
                "xpath=" + MODAL_ROOT + "//button[contains(@class,'ant-modal-close')]", "syncCloseBtn");
    }

    public SyncUpdatesDialogComponent waitForVisible() {
        receiveBtn.waitForVisible();
        return this;
    }

    private static final String DIALOG_JS =
            "const anchor = document.querySelector('[data-testid=merge-target-branch]');"
                    + "const dialog = anchor ? anchor.closest('.ant-modal-container') || anchor.closest('.ant-modal')"
                    + " : null;";

    public long getBodyHorizontalOverflowPx() {
        Object overflow = DriverPool.getPage().evaluate(
                "() => {" + DIALOG_JS
                        + " const body = dialog && dialog.querySelector('.ant-modal-body');"
                        + " return body ? body.scrollWidth - body.clientWidth : -1; }");
        return ((Number) overflow).longValue();
    }

    public String getBranchFieldRowFlexWrap() {
        Object flexWrap = DriverPool.getPage().evaluate(
                "() => {" + DIALOG_JS
                        + " const row = anchor && anchor.closest('.ant-form-item-row');"
                        + " return row ? getComputedStyle(row).flexWrap : null; }");
        if (flexWrap == null) {
            throw new IllegalStateException(
                    "The branch select or its form row was not found in the Sync updates dialog");
        }
        return String.valueOf(flexWrap);
    }

    public boolean isFullBranchNameExposedAsTitle(String branchName) {
        Object found = DriverPool.getPage().evaluate(
                "branch => {" + DIALOG_JS
                        + " return !!(dialog && [...dialog.querySelectorAll('[title]')]"
                        + ".some(node => node.getAttribute('title') === branch)); }",
                branchName);
        return (Boolean) found;
    }

    public String getScopeProjectSummary() {
        return waitForScope().getInnerText().replaceAll("\\s+", " ").trim();
    }

    public List<String> getFilesChangedInBoth() {
        waitForScope();
        return changedInBoth().all().stream()
                .map(file -> {
                    String name = file.locator("xpath=.//strong").innerText().trim();
                    Locator folder = file.locator("xpath=.//span[contains(@class,'ant-typography-secondary')]");
                    return folder.count() == 0 ? name : folder.first().innerText().trim() + "/" + name;
                })
                .toList();
    }

    public String getMergeEstimateOf(String fileName) {
        waitForScope();
        return DriverPool.getPage().locator("xpath=" + CHANGED_IN_BOTH + "[.//strong[normalize-space()=\"" + fileName + "\"]]"
                + "//*[@data-testid='file-conflict' or @data-testid='file-merges-by-itself']").innerText().trim();
    }

    public String getConflictsNote() {
        waitForScope();
        return new WebElement(DriverPool.getPage(), "xpath=" + MODAL_ROOT
                + "//*[@data-testid='merge-scope-conflicts' or @data-testid='merge-scope-no-conflicts']", "mergeScopeConflictsNote")
                .waitForVisible(SCOPE_SHOWN_MS).getInnerText().replaceAll("\\s+", " ").trim();
    }

    private Locator changedInBoth() {
        return DriverPool.getPage().locator("xpath=" + CHANGED_IN_BOTH);
    }

    private WebElement waitForScope() {
        return new WebElement(DriverPool.getPage(), "xpath=(" + SCOPE_PROJECT + ")[1]/div[1]", "mergeScopeProjectSummary")
                .waitForVisible(SCOPE_SHOWN_MS);
    }

    public String getHeader() {
        return modalTitle.getText().trim();
    }

    public boolean isReceiveEnabled() {
        return receiveBtn.isEnabled();
    }

    public boolean isSendEnabled() {
        return sendBtn.isEnabled();
    }

    public void clickReceive() {
        receiveBtn.click();
    }

    public void clickSend() {
        sendBtn.click();
    }

    public void close() {
        closeBtn.click();
    }
}
