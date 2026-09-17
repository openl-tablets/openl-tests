package domain.ui.webstudio.components.editortabcomponents;

import com.microsoft.playwright.Locator;

import java.util.ArrayList;
import java.util.List;

/**
 * What the compilation says, read off the list the screens share.
 *
 * <p>A message stands in a line of its own together with what the screen offers to do about it: the rule it
 * was raised against, a word to ask the server for the stack by, a way to correct the cell it names. Those
 * are drawn beside the message, so the message alone is read.
 *
 * <p>A message too long to stand in the panel is cut, with a word to read the rest by written inside it.
 * That word is pressed before the message is read, so what is read is the whole of it.
 */
final class CompileMessageReader {

    private static final String MESSAGE = "xpath=./*[@data-testid][1]";
    private static final String MESSAGES = "xpath=./li/*[@data-testid][1]";
    private static final String OFFER = "xpath=.//button";

    private CompileMessageReader() {
    }

    /** Every message of one list, in the order the panel lists them. */
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

    /** What one line of the list says. */
    static String textOf(Locator row) {
        return wholeOf(row.locator(MESSAGE));
    }

    private static String wholeOf(Locator message) {
        Locator offer = message.locator(OFFER);
        if (offer.count() > 0) {
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
