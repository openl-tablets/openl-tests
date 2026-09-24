package domain.ui.webstudio.components.editortabcomponents.toolbar;

import java.util.List;

public interface IRunMenu {
    IRunMenu clickCreateItem();
    IRunMenu clickAddElementToCollectionBtn(String parameterName);
    IRunMenu clickExpandCollection();
    IRunMenu clickRunInsideMenu();
    IRunMenu clickAddedElementsExpander(String parameterName);
    List<String> getAliasDropdownValues();
    List<String> getElementsOf(String parameterName);
    boolean offersTheFirstElementAsAList();
    IRunMenu setInputTextField(String index, String value);
    IRunMenu setInputSelectField(String index, String value);
}
