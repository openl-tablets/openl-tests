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
    IRunMenu unfoldParameter(String name);
    boolean offersParameter(String name);
    List<String> getFieldChoices(String name);
    IRunMenu chooseFieldValue(String name, String value);
    IRunMenu writeFieldValue(String name, String value);
    boolean isFieldEditedAsTextOrList(String name);
    IRunMenu clearFieldValue(String name);
    String getFieldValue(String name);
    IRunMenu showJsonInput();
    IRunMenu showFormInput();
    String getJsonInput();
    IRunMenu pasteJsonInput(String json);
}
