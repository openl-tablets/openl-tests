package domain.ui.webstudio.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import configuration.core.ui.CorePage;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.ScreenWindows;
import domain.ui.webstudio.components.common.MessageComponent;
import domain.ui.webstudio.components.common.UserSlidingRightMenuComponent;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class BasePage extends CorePage {

    private static final long ERROR_WATCH_MS = 3000;
    private static final long ERROR_POLL_MS = 250;
    private static final int ERROR_READ_TIMEOUT_MS = 500;
    private static final String SHOWN_ERRORS = "xpath="
            + "//div[" + hasClass("ant-notification-notice-error") + "]"
            + " | " + statusPage("500", "Internal server error.")
            + " | " + statusPage("404", "Page not found.")
            + " | " + statusPage("403", "Access denied.")
            + " | //div[" + hasClass("ant-result-error") + "]"
            + " | //div[h2[normalize-space(.)='Oops! Something went wrong']]"
            + " | //div[" + hasClass("ant-alert-error") + "][" + testIdEndsWith("-error") + " or " + testIdEndsWith("-failed") + "]";

    private WebElement userLogo;
    @Getter
    private List<MessageComponent> messages;
    private WebElement shownErrors;
    private WebElement userMenuDrawer;
    private WebElement contentLoadingSpinner;
    @Getter
    private WebElement modalOkBtn;
    private WebElement notificationPanel;
    private WebElement closableMessage;

    public BasePage() {
        super();
        initializeComponents();
    }

    public BasePage(Page page) {
        super(page);
        initializeComponents();
    }

    private void initializeComponents() {
        userLogo = new WebElement(page, "xpath=//span[contains(@class,'ant-avatar')][.//span[@aria-label='user']]"
                + "[not(ancestor::div[contains(@class,'ant-drawer')])]", "User Logo");
        messages = createComponentList(MessageComponent.class, "xpath=//div[contains(@class,'ant-notification-notice-wrapper')]", "Studio Messages");
        shownErrors = new WebElement(page, SHOWN_ERRORS, "Errors Shown To The User");
        userMenuDrawer = new WebElement(page, "xpath=//div[contains(@class,'ant-drawer') and contains(@class,'ant-drawer-open')]//div[contains(@class,'ant-drawer-section')]", "User Menu Drawer");
        contentLoadingSpinner = new WebElement(page, "xpath=//div[@id='loadingPanel']", "contentLoadingSpinner");
        modalOkBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-modal-container')]//button[./span[contains(text(),'OK')]]", "applyChangesBtn");
        notificationPanel = new WebElement(page, "xpath=//div[@data-show='true' and contains(@class, 'ant-alert-banner')]", "Notification Panel");
        closableMessage = new WebElement(page, "xpath=//div[contains(@class, 'message closable')]", "closableMessage");
    }

    public void clickModalOkBtn() {
        modalOkBtn.waitForVisible();
        modalOkBtn.click();
    }

    public void closeAllMessages() {
        try {
            LOGGER.info("Messages currently open: {}", messages.size());
        } catch (Exception e) {
            LOGGER.debug("Could not get messages size (likely due to DOM update)");
        }
        for(int i = 0; i < 3; i++) {
            try {
                messages.forEach(MessageComponent::closeMessage);
            } catch (Exception e) {
                LOGGER.debug("Ignoring exception during message closing (likely due to DOM update): {}", e.getMessage());
            }
            WaitUtil.sleep(100, "Waiting between message close attempts");
        }
    }

    public List<String> getAllMessages() {
        List<String> messagesTextList = new ArrayList<>();
        for(int i = 0; i < 30; i++) {
            try {
                messages.forEach(m -> {
                    if(!messagesTextList.contains(m.getMessageText()))
                        messagesTextList.add(m.getMessageText());
                });
            } catch (Exception e) {
                LOGGER.debug("Ignoring exception during message collection (likely due to DOM update): {}", e.getMessage());
            }
            WaitUtil.sleep(50, "Waiting between message get_text attempts");
        }
        return messagesTextList;
    }

    public List<String> getShownErrors() {
        WebElement.waitForAppReady(page);
        Set<String> shown = new LinkedHashSet<>();
        int passesRead = 0;
        PlaywrightException lastFailure = null;
        long deadline = System.currentTimeMillis() + ERROR_WATCH_MS;
        do {
            try {
                for (Locator error : shownErrors.getLocator().all()) {
                    if (error.isVisible()) {
                        shown.add(normalized(error.innerText(new Locator.InnerTextOptions().setTimeout(ERROR_READ_TIMEOUT_MS))));
                    }
                }
                passesRead++;
            } catch (PlaywrightException e) {
                lastFailure = e;
                LOGGER.debug("Ignoring exception during error collection (likely due to DOM update): {}", e.getMessage());
            }
            WaitUtil.sleep(ERROR_POLL_MS, "Watching for an error shown to the user");
        } while (System.currentTimeMillis() < deadline);
        if (passesRead == 0) {
            throw new IllegalStateException("The errors shown to the user could not be read", lastFailure);
        }
        LOGGER.info("Errors shown to the user: {}", shown);
        return new ArrayList<>(shown);
    }

    private static String normalized(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static String hasClass(String className) {
        return "contains(concat(' ',normalize-space(@class),' '),' " + className + " ')";
    }

    private static String testIdEndsWith(String suffix) {
        return "substring(@data-testid,string-length(@data-testid)-" + (suffix.length() - 1) + ")='" + suffix + "'";
    }

    private static String statusPage(String code, String message) {
        return "//div[normalize-space(.)='" + code + "'][following-sibling::div[1][starts-with(normalize-space(.),'" + message + "')]]/..";
    }

    public UserSlidingRightMenuComponent openUserMenu() {
        closeAllMessages();
        ScreenWindows.closeAll(getPage());
        userLogo.click();
        userMenuDrawer.waitForVisible();
        return new UserSlidingRightMenuComponent(userMenuDrawer);
    }

    public void waitUntilSpinnerLoaded() {
        contentLoadingSpinner.waitForHidden(DEFAULT_TIMEOUT_MS * 100L);
        WebElement.waitForAppReady(page);
    }

    public boolean waitUntilAppIdle() {
        return WebElement.waitForAppIdle(page, 30000L);
    }

    public boolean hasHorizontalScroll() {
        waitUntilSpinnerLoaded();
        BoundingBox body = page.locator("xpath=//body").boundingBox();
        int viewportWidth = page.viewportSize().width;
        return body != null && body.width > viewportWidth + 1;
    }

    public boolean isNotificationVisible() {
        return notificationPanel.sleep(500).isVisible();
    }

    public boolean isNotificationVisible(int timeoutMillis) {
        return notificationPanel.isVisible(timeoutMillis);
    }

    public String getNotificationText() {
        WaitUtil.waitForCondition(() -> notificationPanel.isVisible(), 100, 1000, "Waiting for notification to be visible");
        if (isNotificationVisible()) {
            return notificationPanel.getText();
        }
        throw new RuntimeException("No Notification text found!");
    }

    public boolean isClosableMessageVisible() {
        return closableMessage.isVisible(1000);
    }

    public String getClosableMessageText() {
        closableMessage.waitForVisible(DEFAULT_TIMEOUT_MS);
        return closableMessage.getText().trim();
    }

    public void closeClosableMessage() {
        if (isClosableMessageVisible()) {
            closableMessage.click();
        }
    }
}
