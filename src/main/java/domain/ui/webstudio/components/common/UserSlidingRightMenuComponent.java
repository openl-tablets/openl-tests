package domain.ui.webstudio.components.common;

import configuration.core.ui.WebElement;
import configuration.driver.DriverPool;
import domain.ui.webstudio.components.BaseComponent;
import domain.ui.webstudio.pages.mainpages.AdminPage;

public class UserSlidingRightMenuComponent extends BaseComponent {

    private WebElement myProfileMenuItem;
    private WebElement administrationMenuItem;
    private WebElement helpMenuItem;
    private WebElement signOutMenuItem;

    public UserSlidingRightMenuComponent() {
        super(DriverPool.getPage());
        initializeComponents();
    }
    
    public UserSlidingRightMenuComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeComponents();
    }

    private void initializeComponents() {
        myProfileMenuItem = createScopedElement("xpath=.//li[contains(@class,'ant-menu-item')]//span[text()='My Profile']", "My Profile Menu Item");
        administrationMenuItem = createScopedElement("xpath=.//li[contains(@class,'ant-menu-item')]//span[text()='Administration']", "Administration Menu Item");
        helpMenuItem = createScopedElement("xpath=.//li[contains(@class,'ant-menu-item')]//span[text()='Help']", "Help Menu Item");
        signOutMenuItem = createScopedElement("xpath=.//li[contains(@class,'ant-menu-item')]//span[text()='Sign Out']", "Sign Out Menu Item");
    }

    public AdminPage navigateToMyProfile() {
        select(MenuElements.MY_PROFILE);
        return new AdminPage();
    }

    public AdminPage navigateToAdministration() {
        select(MenuElements.ADMINISTRATION);
        return new AdminPage();
    }

    public void openHelp() {
        select(MenuElements.HELP);
    }

    public boolean isAdministrationMenuItemVisible() {
        return administrationMenuItem.isVisible(1000);
    }

    public void signOut() {
        select(MenuElements.SIGN_OUT);
    }

    public void close() {
        WebElement closeBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer-open')]"
                + "//button[contains(@class,'ant-drawer-close')]", "Close User Menu");
        closeBtn.click();
        closeBtn.waitForHidden(DEFAULT_TIMEOUT_MS);
    }

    private void select(MenuElements element) {
        switch (element) {
            case MY_PROFILE:
                myProfileMenuItem.click();
                break;
            case ADMINISTRATION:
                administrationMenuItem.click();
                break;
            case HELP:
                helpMenuItem.click();
                break;
            case SIGN_OUT:
                signOutMenuItem.click();
                break;
        }
    }

    public enum MenuElements {
        MY_PROFILE("My Profile"),
        ADMINISTRATION("Administration"),
        HELP("Help"),
        SIGN_OUT("Sign Out");

        private String value;

        MenuElements(String value) {
            this.value = value;
        }
    }
}