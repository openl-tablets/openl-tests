package domain.ui.webstudio.components.common;

import com.microsoft.playwright.Page;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.BaseComponent;

public class HeaderSearchComponent extends BaseComponent {

    public enum Scope {
        PROJECTS("projects"),
        FILES("files"),
        REVISIONS("revisions"),
        TABLES("tables"),
        USERS("users"),
        DEPLOYMENTS("deployments");

        private final String id;

        Scope(String id) {
            this.id = id;
        }
    }

    private final WebElement field;
    private final WebElement input;
    private final WebElement action;

    public HeaderSearchComponent(Page page, Scope scope) {
        super(page);
        String fieldXpath = "xpath=//*[@data-testid='header-search'][@data-scope='" + scope.id + "']";
        field = new WebElement(page, fieldXpath, "headerSearch " + scope.id);
        input = new WebElement(page, fieldXpath + "//input[@data-testid='header-search-input']", "headerSearchInput " + scope.id);
        action = new WebElement(page, fieldXpath + "//button[@data-testid='header-search-action']", "headerSearchAction " + scope.id);
    }

    public boolean isOffered(int timeoutInMillis) {
        return field.isVisible(timeoutInMillis);
    }

    public void waitForOffered(int timeoutInMillis) {
        field.waitForVisible(timeoutInMillis);
    }

    public HeaderSearchComponent search(String text) {
        input.waitForVisible(DEFAULT_TIMEOUT_MS);
        input.fill(text);
        return this;
    }

    public String getText() {
        return input.getCurrentInputValue();
    }

    public boolean isActionOffered(int timeoutInMillis) {
        return action.isVisible(timeoutInMillis);
    }

    public void pressAction() {
        action.click();
    }
}
