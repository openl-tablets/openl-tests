package domain.ui.webstudio.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import configuration.core.ui.CoreComponent;
import configuration.core.ui.WebElement;
import domain.ui.webstudio.components.common.MessageComponent;
import helpers.utils.WaitUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseComponent extends CoreComponent {

    private static final int SELECT_OPTION_PROBE_MS = 2000;
    private static final int OPEN_LIST_PROBE_MS = 500;
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
            WebElement option = optionOf(selectInput, value);
            if (option == null || !option.isVisible(SELECT_OPTION_PROBE_MS)) {
                return false;
            }
            option.click();
            return true;
        }, DEFAULT_TIMEOUT_MS, SELECT_SETTLE_MS, "Picking '" + value + "' from the list");
    }

    /**
     * The value in the list that stands open.
     *
     * <p>A list is drawn outside the box it belongs to, and a list that was open a moment ago is left in the
     * page while it folds away — still readable, and first in the page. So the box names the list it has
     * open, and only that list is read; where the box names none, every list that is not yet marked as
     * folded away is read, and the value taken is the one a reader could point at.
     *
     * <p>A value is carried as the option's title where the list is written with plain words, and only as
     * the words themselves where it is written with anything else. The box the words sit in reads as an
     * option too, so it is left out.
     */
    private WebElement optionOf(WebElement selectInput, String value) {
        String openList = openListIdOf(selectInput);
        if (openList == null || openList.isEmpty()) {
            return null;
        }
        String list = "//div[contains(@class,'ant-select-dropdown')]"
                + "[not(contains(@class,'ant-select-dropdown-hidden'))]"
                + "[.//*[@id=\"" + openList + "\"]]"
                + "//div[contains(@class,'ant-select-item-option')]"
                + "[not(contains(@class,'ant-select-item-option-content'))]";
        WebElement named = drawnOption(list + "[@title=\"" + value + "\" or normalize-space(.)=\"" + value + "\"]", value);
        return named != null ? named : drawnOption(list + "[contains(normalize-space(.),\"" + value + "\")]", value);
    }

    /** The list the box has open, which it names only while it is open. */
    private String openListIdOf(WebElement selectInput) {
        try {
            return selectInput.getLocator()
                    .getAttribute("aria-controls", new Locator.GetAttributeOptions().setTimeout(OPEN_LIST_PROBE_MS));
        } catch (PlaywrightException notOpen) {
            return null;
        }
    }

    /** The first of those values a reader could point at, or none where the list has not drawn it yet. */
    private WebElement drawnOption(String candidates, String value) {
        int drawn = page.locator("xpath=" + candidates).count();
        for (int index = 0; index < drawn; index++) {
            WebElement option = new WebElement(page, "xpath=(" + candidates + ")[" + (index + 1) + "]",
                    "selectOption[" + value + "][" + (index + 1) + "]");
            if (option.isVisible(SELECT_OPTION_PROBE_MS / 4)) {
                return option;
            }
        }
        return null;
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