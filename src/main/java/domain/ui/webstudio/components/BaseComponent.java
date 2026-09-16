package domain.ui.webstudio.components;

import com.microsoft.playwright.Page;
import configuration.core.ui.CoreComponent;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.common.MessageComponent;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseComponent extends CoreComponent {

    private static final int SELECT_OPTION_PROBE_MS = 2000;
    private static final int SELECT_SETTLE_MS = 250;

    private WebElement contentLoadingSpinner;
    @Getter
    private List<MessageComponent> messages;
    @Getter
    private WebElement modalOkBtn;
    private WebElement notificationPanel;

    public BaseComponent(Page page) {
        super(page);
        initializeElements();
    }

    public BaseComponent(WebElement rootLocator) {
        super(rootLocator);
        initializeElements();
    }

    private void initializeElements() {
        contentLoadingSpinner = new WebElement(page, "xpath=//div[@id='loadingPanel']", "contentLoadingSpinner");
        messages = createComponentList(MessageComponent.class, "xpath=//div[contains(@class,'ant-notification-notice-wrapper')]", "Studio Messages");
        modalOkBtn = new WebElement(page, "xpath=//div[contains(@class,'ant-modal-container')]//button[./span[contains(text(),'OK')]]", "applyChangesBtn");
        notificationPanel = new WebElement(page, "xpath=//div[@data-show='true' and contains(@class, 'ant-alert-banner')]", "Notification Panel");
    }

    public void waitUntilSpinnerLoaded() {
        contentLoadingSpinner.waitForHidden(30000);
        WebElement.waitForAppReady(page);
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

    /**
     * Picks a value from an Ant Design Select, whatever screen it stands on.
     *
     * <p>A Select that takes typing is narrowed by the value first: a long list is drawn a screenful at a
     * time, so the value may not be drawn until it is the only one left.
     */
    protected void pickInSelect(WebElement selectInput, String value) {
        WaitUtil.requireCondition(() -> {
            // Pressing an open list closes it again, so it is opened only while it stands closed.
            if (!"true".equals(selectInput.getAttribute("aria-expanded"))) {
                selectInput.click();
            }
            if (selectInput.getAttribute("readonly") == null) {
                selectInput.fill("");
                page.keyboard().type(value);
            }
            WebElement option = optionOf(value);
            if (!option.isVisible(SELECT_OPTION_PROBE_MS)) {
                return false;
            }
            option.click();
            return true;
        }, DEFAULT_TIMEOUT_MS, SELECT_SETTLE_MS, "Picking '" + value + "' from the list");
    }

    /**
     * The value in the list that stands open. The list is drawn outside the Select, and a list closed before
     * it stays in the page marked as hidden, so only a list that is not hidden is read.
     */
    private WebElement optionOf(String value) {
        return new WebElement(page, "xpath=(//div[contains(@class,'ant-select-dropdown')]"
                + "[not(contains(@class,'ant-select-dropdown-hidden'))]"
                + "//div[contains(@class,'ant-select-item-option')][@title=\"" + value + "\"])[1]",
                "selectOption[" + value + "]");
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

    public List<String> getAllMessagesFullText() {
        List<String> messagesTextList = new ArrayList<>();
        try {
            messages.forEach(m -> {
                String text = m.getFullText();
                if (!text.isEmpty() && !messagesTextList.contains(text)) {
                    messagesTextList.add(text);
                }
            });
        } catch (Exception e) {
            LOGGER.debug("Ignoring exception during message collection (likely due to DOM update): {}", e.getMessage());
        }
        LOGGER.info("Popup messages on screen: {}", messagesTextList);
        return messagesTextList;
    }

    public boolean isNotificationVisible() {
        return notificationPanel.sleep(500).isVisible();
    }

    public boolean isNotificationHidden(int timeoutMillis) {
        try {
            notificationPanel.waitForHidden(timeoutMillis);
            return true;
        } catch (Exception e) {
            return !notificationPanel.isVisible();
        }
    }

    public String getNotificationText() {
        WaitUtil.waitForCondition(() -> notificationPanel.isVisible(), 100, 1000, "Waiting for notification to be visible");
        if (isNotificationVisible()) {
            return notificationPanel.getText();
        }
        throw new RuntimeException("No Notification text found!");
    }
}