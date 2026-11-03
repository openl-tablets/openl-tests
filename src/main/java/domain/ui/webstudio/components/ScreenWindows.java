package domain.ui.webstudio.components;

import com.microsoft.playwright.Page;
import domain.ui.webstudio.components.editortabcomponents.ChangesDialogComponent;
import domain.ui.webstudio.components.editortabcomponents.EditorRevisionsTabComponent;
import domain.ui.webstudio.components.editortabcomponents.TestResultValidationComponent;

public final class ScreenWindows {

    private static final String ANY_WINDOW = "xpath=//div[contains(@class,'ant-modal-wrap')]"
            + "[not(contains(@style,'display: none'))]";

    private ScreenWindows() {
    }

    public static void closeAll(Page page) {
        if (page.locator(ANY_WINDOW).count() == 0) {
            return;
        }
        new ChangesDialogComponent().closeIfOpen();
        new EditorRevisionsTabComponent().closeIfOpen();
        new TestResultValidationComponent().closeResultsOverTheEditor();
    }
}
