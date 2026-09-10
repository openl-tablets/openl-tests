package domain.ui.webstudio.components.repositorytabcomponents;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;

// React "Sync updates" dialog (build 032c60a664ce+), opened from the project-detail Branches tab via a
// branch row's Merge action. Replaces the legacy SyncChangesDialogComponent. The merge target is implicit
// (the branch whose Merge action was clicked) — there is no branch dropdown. Two actions: "Receive their
// updates" (import) and "Send your updates" (export), each disabled when the branches are already in sync.
public class SyncUpdatesDialogComponent extends BaseComponent {

    private static final String MODAL_ROOT =
            "//div[contains(@class,'ant-modal')][.//div[contains(@class,'ant-modal-title') and contains(normalize-space(),'Sync updates')]]";

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

    // A long branch name used to widen the control past the 600px dialog: the label wrapped onto its own
    // line and the dialog scrolled sideways. These read what the browser computed, since the fix is a
    // layout one and there is nothing else observable about it.
    // Anchored on the branch select, which exists in builds with and without EPBDS-16462 and carries no
    // translatable text. Anchoring on a button label would tie these checks to the English locale, and
    // anchoring on merge-target-branch-field would tie them to a test hook the fix itself introduced -
    // the check would then pass or fail on the presence of that hook rather than on the layout.
    private static final String DIALOG_JS =
            "const anchor = document.querySelector('[data-testid=merge-target-branch]');"
                    + "const dialog = anchor ? anchor.closest('.ant-modal-content') || anchor.closest('.ant-modal')"
                    + " : null;";

    // How far the dialog body overflows its own box, in pixels. Returns -1 when the dialog cannot be found,
    // so a missing dialog fails an assertion with a distinguishable value instead of reading as "it fits".
    public long getBodyHorizontalOverflowPx() {
        Object overflow = DriverPool.getPage().evaluate(
                "() => {" + DIALOG_JS
                        + " const body = dialog && dialog.querySelector('.ant-modal-body');"
                        + " return body ? body.scrollWidth - body.clientWidth : -1; }");
        return ((Number) overflow).longValue();
    }

    // The row that holds the branch select. Without the fix it wraps, putting the label on its own line.
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

    public String getHeader() {
        return modalTitle.getText().trim();
    }

    // "Receive their updates" = pull the target branch's changes into the current branch (legacy import).
    public boolean isReceiveEnabled() {
        return receiveBtn.isEnabled();
    }

    // "Send your updates" = push the current branch's changes to the target branch (legacy export).
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
