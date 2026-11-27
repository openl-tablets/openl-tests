package domain.ui.webstudio.components.projectdetail;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;

public class ProjectHeaderActionsComponent extends BaseComponent {

    private static final int PROBE_MS = DEFAULT_TIMEOUT_MS / 5;

    private static final String ACTION_OF = "//button[starts-with(@data-testid,'%s-') and not(substring(@data-testid, string-length(@data-testid) - 4) = '-more')]";

    private final WebElement actionByLabel;
    private final WebElement moreBtn;
    private final WebElement overflowAction;

    public ProjectHeaderActionsComponent(Page page) {
        this(new WebElement(page, "xpath=//*[@data-testid='project-actions']", "projectHeaderActions"));
    }

    public ProjectHeaderActionsComponent(WebElement rootLocator) {
        super(rootLocator);
        actionByLabel = createScopedElement("xpath=." + ACTION_OF, "headerAction");
        moreBtn = createScopedElement("xpath=.//*[@data-testid='project-actions-more']", "headerMoreBtn");
        overflowAction = new WebElement(page, "xpath=//div[@data-testid='project-actions-overflow']" + ACTION_OF, "headerOverflowAction");
    }

    public void clickAction(String actionLabel) {
        WebElement action = actionByLabel.format(actionIdOf(actionLabel));
        if (action.isVisible(PROBE_MS)) {
            LOGGER.info("Header action '{}' clicked on the bar", actionLabel);
            action.click();
            return;
        }
        LOGGER.info("Header action '{}' is not on the bar; opening the Actions menu", actionLabel);
        moreBtn.click();
        overflowAction.format(actionIdOf(actionLabel)).click();
    }

    public boolean isActionEnabled(String actionLabel) {
        WebElement action = actionByLabel.format(actionIdOf(actionLabel));
        if (action.isVisible(PROBE_MS)) {
            return action.isEnabled();
        }
        if (!moreBtn.isVisible(PROBE_MS) || !moreBtn.isEnabled()) {
            return false;
        }
        moreBtn.click();
        WebElement inMenu = overflowAction.format(actionIdOf(actionLabel));
        boolean enabled = inMenu.isVisible(PROBE_MS) && inMenu.isEnabled();
        page.keyboard().press("Escape");
        return enabled;
    }

    public boolean isActionAvailable(String actionLabel) {
        WebElement action = actionByLabel.format(actionIdOf(actionLabel));
        if (action.isVisible(PROBE_MS)) {
            return true;
        }
        if (!moreBtn.isVisible(PROBE_MS)) {
            return false;
        }
        moreBtn.click();
        boolean inOverflow = overflowAction.format(actionIdOf(actionLabel)).isVisible(PROBE_MS);
        page.keyboard().press("Escape");
        return inOverflow;
    }

    private static String actionIdOf(String actionLabel) {
        return switch (actionLabel) {
            case "Open Revision" -> "openRevision";
            case "Delete Branch" -> "deleteBranch";
            default -> actionLabel.substring(0, 1).toLowerCase() + actionLabel.substring(1);
        };
    }
}
