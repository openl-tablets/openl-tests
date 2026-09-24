package domain.ui.webstudio.components.editortabcomponents.toolbar;

import java.util.List;

public interface ITraceMenu {
    ITraceMenu setFactorTextField(String text);
    ITraceMenu setParameterField(String parameterName, String value);
    ITraceMenu clickCreateItem();
    ITraceMenu clickAddElementToCollectionBtn(String parameterName);
    ITraceMenu clickExpandCollection();
    ITraceMenu selectJSONTrace(String json);
    ITraceMenu clickTraceIntoFile();
    ITraceWindow clickTraceInsideMenu();
    ITraceWindow clickTraceInsideMenu(boolean isPopupExpected);
    ITraceWindow clickTraceInsideMenuBusiness();
    List<String> getAliasDropdownValues();
    List<String> getElementsOf(String parameterName);
    boolean offersTheFirstElementAsAList();
}
