package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;

import java.util.ArrayList;
import java.util.List;

final class CompileMessageReader {

    private static final String MESSAGE = "xpath=./*[@data-testid][1]";
    private static final String MESSAGES = "xpath=./li/*[@data-testid][1]";
    private static final String OFFER = "xpath=.//button";

    private CompileMessageReader() {
    }

    static List<String> textsOf(Locator list) {
        Locator messages = list.locator(MESSAGES);
        if (messages.locator(OFFER).count() == 0) {
            return messages.allInnerTexts().stream().map(String::trim).toList();
        }
        List<String> read = new ArrayList<>();
        for (int index = 0; index < messages.count(); index++) {
            read.add(wholeOf(messages.nth(index)));
        }
        return read;
    }

    static String textOf(Locator row) {
        return wholeOf(row.locator(MESSAGE));
    }

    private static String wholeOf(Locator message) {
        Locator offer = message.locator(OFFER);
        if (offer.count() > 0 && !"Show less".equals(offer.first().innerText().trim())) {
            offer.first().click();
        }
        String read = message.innerText().trim();
        if (offer.count() == 0) {
            return read;
        }
        String word = offer.first().innerText().trim();
        int at = read.lastIndexOf(word);
        return at < 0 ? read : read.substring(0, at).trim();
    }
}
