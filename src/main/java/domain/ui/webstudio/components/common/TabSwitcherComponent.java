package domain.ui.webstudio.components.common;

import domain.ui.webstudio.components.BaseComponent;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.pages.BasePage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.List;

public class TabSwitcherComponent extends BaseComponent {

    private static final int LIST_PROBE_MS = 1500;

    private WebElement tabLabelTemplate;
    private WebElement projectsList;
    private WebElement projectsCrumb;

    public TabSwitcherComponent() {
        super(DriverPool.getPage());
        initializeElements();
    }

    public TabSwitcherComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        tabLabelTemplate = createScopedElement("xpath=./li[./span[text()='%s']]/span", "selectedTabLabel");
        projectsList = new WebElement(page, "xpath=//div[@data-testid='projects-home']", "projectsList");
        // Inside a project the breadcrumb leads back to the list as well. The name of the application leads
        // there too, from every screen, so the one meant here is named inside the screen's own header.
        projectsCrumb = new WebElement(page, "xpath=//div[@data-testid='module-header']//a[@href='/projects']"
                + " | //div[@data-testid='project-header']//a[@href='/projects']", "projectsCrumb");
    }

    public List<String> getVisibleTabNames() {
        return rootLocator.getLocator().locator("xpath=./li//span[not(*)]")
                .allInnerTexts().stream().map(String::trim).filter(name -> !name.isEmpty()).toList();
    }

    public boolean isTabOfferedWithin(String tabName, long timeoutMs) {
        return WaitUtil.waitForCondition(() -> getVisibleTabNames().contains(tabName),
                timeoutMs, 500, "Waiting for the '" + tabName + "' tab to be offered");
    }

    /**
     * Goes to the screen the tab names, which for Projects is the list of them.
     *
     * <p>A project and a module of it are drawn under the same tab as the list, so while either is open the
     * tab is already the one standing out. Pressing it is still how a reader comes back to the list, so the
     * press is made whatever the tab looks like and what is waited for is the list itself.
     */
    @SuppressWarnings("unchecked")
    public <T extends BasePage> T selectTab(TabName tabName) {
        WebElement tabLabel = tabLabelTemplate.format(tabName.getValue());
        WaitUtil.requireCondition(() -> {
            if (projectsList.isVisible(LIST_PROBE_MS)) {
                return true;
            }
            if (tabLabel.isVisible(LIST_PROBE_MS)) {
                tabLabel.click();
            } else if (projectsCrumb.isVisible(LIST_PROBE_MS)) {
                projectsCrumb.click();
            }
            return projectsList.isVisible(LIST_PROBE_MS);
        }, DEFAULT_TIMEOUT_MS, 500, "Going to '" + tabName.getValue() + "'");
        waitUntilSpinnerLoaded();

        return switch (tabName) {
            case REPOSITORY -> (T) new RepositoryPage();
        };
    }

    @Getter
    public enum TabName {
        REPOSITORY("Projects");

        private String value;

        TabName(String value) {
            this.value = value;
        }
    }
}