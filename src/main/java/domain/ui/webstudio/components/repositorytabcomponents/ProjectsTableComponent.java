package domain.ui.webstudio.components.repositorytabcomponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;
import helpers.utils.WaitUtil;

import java.util.ArrayList;
import java.util.List;

public class ProjectsTableComponent extends BaseComponent {

    private static final String OVERFLOW_TRIGGER_LABEL = "Actions";
    private static final int PROBE_MS = DEFAULT_TIMEOUT_MS / 5;
    private static final int ACTION_CLICK_TIMEOUT_MS = DEFAULT_TIMEOUT_MS / 2;
    private static final int ROW_LISTED_TIMEOUT_MS = 90000;

    private final WebElement listBeingRead;
    private final WebElement rowByName;
    private final WebElement nameInRow;
    private final WebElement branchInRow;
    private final WebElement actionByName;
    private final WebElement rowMoreBtn;
    private final WebElement actionByNameAndState;
    private final WebElement rowMoreBtnByState;
    private final WebElement nameInRowByState;
    private final WebElement rowActionButtons;
    private final WebElement overflowMenuItem;
    private final WebElement overflowMenuItems;

    public ProjectsTableComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='projects-table']", "projectsTable"));
    }

    public ProjectsTableComponent(WebElement rootLocator) {
        super(rootLocator);
        listBeingRead = new WebElement(page,
                "xpath=//*[@data-testid='projects-home-loading'] | //*[@data-testid='projects-loading-overlay']",
                "projectsListBeingRead");
        rowByName = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']]", "projectRow");
        nameInRow = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')]//span[normalize-space()='%s']", "projectNameInRow");
        branchInRow = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']]//span[starts-with(@data-testid,'row-branch-')][not(contains(@data-testid,'-default'))][not(contains(@data-testid,'-trigger'))]", "projectBranchInRow");
        actionByName = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']]//button[@aria-label='%s']", "projectRowAction");
        rowMoreBtn = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']]//button[starts-with(@data-testid,'project-actions-')]", "projectRowMoreBtn");
        actionByNameAndState = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']][.//button[@aria-label='%s']]//button[@aria-label='%s']", "projectRowActionByState");
        rowMoreBtnByState = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']][.//button[@aria-label='%s']]//button[starts-with(@data-testid,'project-actions-')]", "projectRowMoreBtnByState");
        nameInRowByState = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//button[@aria-label='%2$s']]//span[normalize-space()='%1$s']", "projectNameInRowByState");
        rowActionButtons = createScopedElement("xpath=.//tr[starts-with(@data-testid,'project-row')][.//span[normalize-space()='%s']]//button[@aria-label]", "projectRowActionButtons");
        overflowMenuItem = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]//li[contains(@class,'ant-dropdown-menu-item')][normalize-space()='%s']", "overflowMenuItem");
        overflowMenuItems = new WebElement(page, "xpath=//div[contains(@class,'ant-dropdown')][not(contains(@class,'ant-dropdown-hidden'))]//li[contains(@class,'ant-dropdown-menu-item')]", "overflowMenuItems");
    }

    public boolean isProjectPresent(String projectName) {
        waitUntilTheListIsDrawn();
        return rowByName.format(projectName).isVisible(DEFAULT_TIMEOUT_MS);
    }

    private void waitUntilTheListIsDrawn() {
        WaitUtil.waitForCondition(() -> !listBeingRead.exists(), DEFAULT_TIMEOUT_MS, 250,
                "Waiting for the list of projects to be drawn");
    }

    public WebElement getRow(String projectName) {
        return rowByName.format(projectName);
    }

    public boolean isInlineActionPresent(String projectName, String actionLabel) {
        return actionByName.format(projectName, actionLabel).getLocator().count() > 0;
    }

    public void waitUntilOpened(String projectName) {
        WaitUtil.waitForCondition(() -> isInlineActionPresent(projectName, "Close"),
                DEFAULT_TIMEOUT_MS, 250, "Waiting for project " + projectName + " to finish opening");
    }

    public void clickProjectName(String projectName) {
        waitUntilTheListIsDrawn();
        WebElement name = nameInRow.format(projectName);
        name.waitForVisible(ROW_LISTED_TIMEOUT_MS);
        name.click();
    }

    public void clickRowByState(String projectName, boolean opened) {
        String action = opened ? "Close" : "Open";
        nameInRowByState.format(projectName, action).click();
    }

    public void clickRowAction(String projectName, String actionLabel) {
        WaitUtil.retryOnException(() -> {
            WebElement inlineAction = actionByName.format(projectName, actionLabel);
            if (inlineAction.isVisible(PROBE_MS)) {
                inlineAction.click(ACTION_CLICK_TIMEOUT_MS);
            } else {
                rowMoreBtn.format(projectName).click();
                overflowMenuItem.format(actionLabel).click(ACTION_CLICK_TIMEOUT_MS);
            }
            return null;
        }, DEFAULT_TIMEOUT_MS * 2, 500, "Pressing the row action '" + actionLabel + "' of project " + projectName);
    }

    public void clickRowActionByState(String projectName, boolean opened, String actionLabel) {
        String stateAction = opened ? "Close" : "Open";
        WebElement inlineAction = actionByNameAndState.format(projectName, stateAction, actionLabel);
        if (inlineAction.isVisible(PROBE_MS)) {
            inlineAction.click();
            return;
        }
        rowMoreBtnByState.format(projectName, stateAction).click();
        overflowMenuItem.format(actionLabel).click();
    }

    public List<String> getProjectActionLabels(String projectName) {
        List<String> labels = new ArrayList<>();
        Locator btns = rowActionButtons.format(projectName).getLocator();
        WaitUtil.waitForCondition(() -> btns.count() > 0, DEFAULT_TIMEOUT_MS, 250, "Waiting for project row actions");
        int count = btns.count();
        for (int i = 0; i < count; i++) {
            String label = btns.nth(i).getAttribute("aria-label");
            if (label != null && !label.isEmpty() && !OVERFLOW_TRIGGER_LABEL.equals(label)) {
                labels.add(label);
            }
        }
        if (rowMoreBtn.format(projectName).isVisible(PROBE_MS)) {
            rowMoreBtn.format(projectName).click();
            Locator items = overflowMenuItems.getLocator();
            WaitUtil.waitForCondition(() -> items.count() > 0, DEFAULT_TIMEOUT_MS, 250, "Waiting for the row overflow menu");
            labels.addAll(items.allInnerTexts().stream().map(String::trim).filter(text -> !text.isEmpty()).toList());
            page.keyboard().press("Escape");
        }
        return labels;
    }

    public String getProjectBranch(String projectName) {
        return branchInRow.format(projectName).waitForVisible(DEFAULT_TIMEOUT_MS).getText().trim();
    }

    public List<String> getAllVisibleProjectNames() {
        List<String> projectNames = new ArrayList<>();
        Locator rows = getRootLocator().getLocator().locator("xpath=.//tr[starts-with(@data-testid,'project-row')]");
        WaitUtil.waitForCondition(() -> rows.count() > 0, 5000, 250, "Waiting for projects to load");
        int count = rows.count();
        for (int i = 0; i < count; i++) {
            if (!rows.nth(i).isVisible()) {
                continue;
            }
            String name = rows.nth(i).locator("xpath=.//span").first().textContent();
            if (name != null && !name.trim().isEmpty()) {
                projectNames.add(name.trim());
            }
        }
        return projectNames;
    }
}
