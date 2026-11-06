package domain.ui.webstudio.components.editortabcomponents.toolbar;

import java.util.List;

public interface ITraceWindow {
    String getStatus();
    List<String> getCallTreeTitles();
    ITraceWindow selectTreeNode(int position);
    ITraceWindow stepOver();
    ITraceWindow resume();
    String getDetailsText();
    String getTracedTableText();
    boolean isNodeDetailsErrorDisplayed(int timeoutInMillis);
    boolean areDetailsDisplayed(int timeoutInMillis);
    boolean isDebugToolbarShown(int timeoutInMillis);
    boolean isBusinessToggleShown(int timeoutInMillis);
    ITraceWindow toggleDetailedTrace();
    String getSimpleTreeText();
    ITraceWindow expandSimpleStep(String step);
    int getSimpleCallsCount(String step);
    String getSimpleMoreCallsText(String step);
    ITraceWindow showMoreSimpleCalls(String step);
    ITraceWindow selectSimpleRow(int position);
    ITraceWindow selectMiddleSimpleRow();
    boolean isSimpleRowFailed(int position);
    String getSimpleRowText(int position);
    ITraceWindow loadFirstDetailsValue();
    ITraceWindow waitForDetailsToContain(String expectedText);
    ITraceWindow setBreakWhenRuleFires();
    ITraceWindow pickBreakOnRule(String ruleName);
    ITraceWindow addWatch(String expression);
    String getWatchPanelText();
    ITraceWindow clickRerun();
    ITraceWindow waitForStatus(String expectedStatus, int timeoutInMillis);
    String getDecisionPanelText();
    ITraceWindow waitForDecisionPanelToContain(String expectedText, int timeoutInMillis);
    void close();
}
