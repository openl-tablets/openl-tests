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
        modalOkBtn = new WebElement(page, "xpath=//div[@class='ant-modal-container']//button[./span[contains(text(),'OK')]]", "applyChangesBtn");
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
     * <p>The list a Select opens is drawn outside it, and the Select says which list is its own through
     * {@code aria-controls}, so the value is looked for in that list rather than in whatever list is open.
     * A Select that takes typing is narrowed by the value first, because a long list is drawn a screenful at
     * a time and the value may not be drawn yet.
     */
    protected void pickInSelect(WebElement selectInput, String value) {
        WaitUtil.requireCondition(() -> {
            selectInput.click();
            String listId = selectInput.getAttribute("aria-controls");
            if (listId == null || listId.isBlank()) {
                return false;
            }
            if (selectInput.getAttribute("readonly") == null) {
                selectInput.fill("");
                page.keyboard().type(value);
            }
            WebElement option = optionOf(listId, value);
            if (!option.isVisible(SELECT_OPTION_PROBE_MS)) {
                return false;
            }
            option.click();
            return true;
        }, DEFAULT_TIMEOUT_MS, SELECT_SETTLE_MS, "Picking '" + value + "' from the list");
    }

    private WebElement optionOf(String listId, String value) {
        return new WebElement(page, "xpath=//div[@id=\"" + listId + "\"]//div[contains(@class,'ant-select-item-option')]"
                + "[@title=\"" + value + "\"]", "selectOption[" + value + "]");
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