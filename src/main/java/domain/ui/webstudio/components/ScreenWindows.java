package domain.ui.webstudio.components;

import com.microsoft.playwright.Page;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;

/**
 * The windows a reader opens over a screen: the module's history, the project's revisions, the report of a
 * run. Nothing underneath can be pressed through one, so they are put away before the screen is worked on
 * again — which is what a reader does.
 */
public final class ScreenWindows {

    private static final String ANY_WINDOW = "xpath=//div[contains(@class,'ant-modal-wrap')]"
            + "[not(contains(@style,'display: none'))]";

    private ScreenWindows() {
    }

    /** Closes what stands over the screen. Nothing is looked for unless a window stands at all. */
    public static void closeAll(Page page) {
        if (page.locator(ANY_WINDOW).count() == 0) {
            return;
        }
        new ChangesDialogComponent().closeIfOpen();
        new EditorRevisionsTabComponent().closeIfOpen();
        new TestResultValidationComponent().closeResults();
    }
}
