package domain.ui.webstudio.components.common;

import domain.ui.webstudio.components.BaseComponent;
import com.microsoft.playwright.PlaywrightException;
import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.pages.BasePage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.List;

public class TabSwitcherComponent extends BaseComponent {

    private static final int LIST_PROBE_MS = 1500;
    private static final int TAB_CLICK_MS = 3000;

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
        projectsList = new WebElement(page, "xpath=(//div[@data-testid='projects-home']//*[@data-testid='projects-table'"
                + " or @data-testid='projects-grid' or @data-testid='projects-empty' or @data-testid='projects-no-match'"
                + " or @data-testid='projects-indexing'])[1]", "projectsList");
        projectsCrumb = new WebElement(page, "xpath=//div[@data-testid='module-header']//a[@href='/projects']"
                + " | //div[@data-testid='project-header']//a[@href='/projects']"
                + " | //div[@data-testid='project-detail']//a[@href='/projects']", "projectsCrumb");
    }

    public List<String> getVisibleTabNames() {
        return rootLocator.getLocator().locator("xpath=./li//span[not(*)]")
                .allInnerTexts().stream().map(String::trim).filter(name -> !name.isEmpty()).toList();
    }

    public boolean isTabOfferedWithin(String tabName, long timeoutMs) {
        return WaitUtil.waitForCondition(() -> getVisibleTabNames().contains(tabName),
                timeoutMs, 500, "Waiting for the '" + tabName + "' tab to be offered");
    }

    @SuppressWarnings("unchecked")
    public <T extends BasePage> T selectTab(TabName tabName) {
        WebElement tabLabel = tabLabelTemplate.format(tabName.getValue());
        WaitUtil.requireCondition(() -> {
            if (projectsList.isVisible(LIST_PROBE_MS)) {
                return true;
            }
            closeWindowsOverTheScreen();
            try {
                if (tabLabel.isVisible(LIST_PROBE_MS)) {
                    tabLabel.click(TAB_CLICK_MS);
                }
                if (projectsList.isVisible(LIST_PROBE_MS)) {
                    return true;
                }
                if (projectsCrumb.isVisible(LIST_PROBE_MS)) {
                    projectsCrumb.click(TAB_CLICK_MS);
                }
            } catch (PlaywrightException covered) {
                LOGGER.info("The way back to the list could not be pressed, trying again: {}", covered.getMessage());
                return false;
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