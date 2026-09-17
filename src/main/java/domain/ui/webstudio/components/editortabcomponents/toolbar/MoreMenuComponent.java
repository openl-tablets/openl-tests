package domain.ui.webstudio.components.editortabcomponents.toolbar;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.CompareExcelFilesDialogComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;

public class MoreMenuComponent extends BaseComponent implements IMoreMenu {

    private static final int MENU_ITEM_VISIBLE_TIMEOUT_MS = 500;
    private static final int MENU_ITEM_CLICK_TIMEOUT_MS = 2000;
    private static final long MENU_RETRY_TIMEOUT_MS = DEFAULT_TIMEOUT_MS * 2L;

    private final WebElement toggle;
    private final WebElement changesBtn;
    private final WebElement revisionsBtn;
    private final WebElement compareExcelFilesBtn;
    private final WebElement tableDependenciesBtn;
    private final WebElement allMenuLinks;
    private final WebElement revisionsTab;

    // The menu itself is rendered into a body-level dropdown, so its items are located at page level.
    private static final String OPEN_MENU = "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]";

    public MoreMenuComponent(Page page) {
        this(new WebElement(page, "xpath=//button[@data-testid='module-more']", "moreBtn"));
    }

    public MoreMenuComponent(WebElement rootLocator) {
        super(rootLocator);
        toggle = rootLocator;
        changesBtn = new WebElement(page, OPEN_MENU + "//li[normalize-space()='Local Changes']", "changesBtn");
        revisionsBtn = new WebElement(page, OPEN_MENU + "//li[normalize-space()='Revisions']", "revisionsBtn");
        compareExcelFilesBtn = new WebElement(page, OPEN_MENU + "//li[normalize-space()='Compare Excel files']", "compareExcelFilesBtn");
        tableDependenciesBtn = new WebElement(page, OPEN_MENU + "//li[normalize-space()='Table Dependencies']", "tableDependenciesBtn");
        allMenuLinks = new WebElement(page, OPEN_MENU + "//li[contains(@class,'ant-dropdown-menu-item')]", "allMoreMenuLinks");
        revisionsTab = new WebElement(page, "xpath=//div[@data-testid='project-tabs']//div[@data-node-key='history']", "revisionsTab");
    }

    /**
     * Opens the More menu. The project's own screen has none — what the menu holds is its tabs there — so
     * the press is made only where the menu is, and each action finds its own way from whichever screen.
     */
    public MoreMenuComponent open() {
        WaitUtil.sleep(1000, "Waiting before opening the More menu");
        if (toggle.isVisible(MENU_ITEM_VISIBLE_TIMEOUT_MS * 2)) {
            toggle.click();
            WaitUtil.sleep(500, "Waiting for the More menu to open");
        }
        return this;
    }

    @Override
    public ChangesDialogComponent clickChanges() {
        waitUntilSpinnerLoaded();
        ChangesDialogComponent changes = new ChangesDialogComponent();
        boolean opened = WaitUtil.retryAction(() -> {
            clickMenuItem(changesBtn, "Local Changes");
            if (!changes.isViewShown(DEFAULT_TIMEOUT_MS)) {
                throw new IllegalStateException("Local Changes view did not open after clicking the menu item");
            }
        }, MENU_RETRY_TIMEOUT_MS + DEFAULT_TIMEOUT_MS, 500, "Opening Local Changes from the More menu");
        if (!opened) {
            throw new IllegalStateException("Local Changes view did not open within " + (MENU_RETRY_TIMEOUT_MS + DEFAULT_TIMEOUT_MS) + " ms");
        }
        return changes.waitForLoaded();
    }

    @Override
    /**
     * Opens what the project has been through. A module screen keeps it behind More; the project's own
     * screen gives it a tab of its own, so which of the two is pressed depends on where the reader stands.
     */
    public void clickRevisions() {
        if (toggle.isVisible(MENU_ITEM_VISIBLE_TIMEOUT_MS * 2)) {
            clickMenuItem(revisionsBtn, "Revisions");
        } else {
            revisionsTab.waitForVisible(DEFAULT_TIMEOUT_MS);
            revisionsTab.click();
        }
        WaitUtil.sleep(500, "Waiting for the revisions to be listed");
    }

    @Override
    public void clickTableDependencies() {
        clickMenuItem(tableDependenciesBtn, "Table Dependencies");
        WaitUtil.sleep(1000, "Waiting for Table Dependencies view to load");
    }

    private void clickMenuItem(WebElement item, String itemName) {
        boolean clicked = WaitUtil.retryAction(() -> {
            if (!item.isVisible(MENU_ITEM_VISIBLE_TIMEOUT_MS)) {
                toggle.click();
            }
            item.click(MENU_ITEM_CLICK_TIMEOUT_MS);
        }, MENU_RETRY_TIMEOUT_MS, 500, "Clicking '" + itemName + "' in the More menu, re-opening the menu if a toolbar refresh closed it");
        if (!clicked) {
            throw new IllegalStateException("'" + itemName + "' in the More menu could not be clicked within " + MENU_RETRY_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public CompareExcelFilesDialogComponent clickCompareExcelFiles() {
        Page popup = page.waitForPopup(compareExcelFilesBtn::click);
        popup.waitForLoadState();
        return new CompareExcelFilesDialogComponent(popup);
    }

    public List<String> getMenuItems() {
        List<String> items = new ArrayList<>();
        Locator menuItems = allMenuLinks.getLocator();
        for (int i = 0; i < menuItems.count(); i++) {
            Locator item = menuItems.nth(i);
            if (item.isVisible()) {
                String text = item.textContent().trim();
                if (!text.isEmpty()) {
                    items.add(text);
                }
            }
        }
        return items;
    }
}
