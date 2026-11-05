package tests.ui.webstudio.repository;

import configuration.annotations.AppContainerConfig;
import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.repositorytabcomponents.ProjectsFilterRailComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.List;

import static domain.ui.webstudio.components.repositorytabcomponents.ProjectsFilterRailComponent.BRANCH_FILTER_GROUP;
import static domain.ui.webstudio.components.repositorytabcomponents.ProjectsFilterRailComponent.REPOSITORY_FILTER_GROUP;
import static domain.ui.webstudio.components.repositorytabcomponents.ProjectsFilterRailComponent.STATUS_FILTER_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

public class TestProjectsFilterRailArrangeUi extends BaseTest {

    private static final String PROJECT = "FilterRailArrange";
    private static final String TEMPLATE = "Sample Project";
    private static final int DROPPED_MS = 5000;

    @Test
    @TestCaseId("EPBDS-16753")
    @Description("EPBDS-16753: in the arrange mode of the Filters view every group shows only its header, and a group "
            + "is dragged with the mouse by the middle of its header, not by its grip, to the last place, to the first "
            + "place and to the middle of the rail; the Done button brings the values back and the order stays.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void filterGroupsAreDraggedByTheWholeHeaderToAnyPlaceOfTheRail() {
        EditorPage editorPage = new LoginService(DriverPool.getPage()).login(UserService.getUser(User.ADMIN));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, PROJECT, TEMPLATE);
        repositoryPage.openProjectsList();
        ProjectsFilterRailComponent rail = repositoryPage.getFilterRail();

        List<String> initialOrder = List.of(REPOSITORY_FILTER_GROUP, BRANCH_FILTER_GROUP, STATUS_FILTER_GROUP);
        assertThat(rail.getGroupOrder())
                .as("Precondition: the rail lists the repository, branch and status groups in this order")
                .containsExactlyElementsOf(initialOrder);
        rail.waitForFilterValues(true);
        assertThat(rail.countFilterValues())
                .as("Precondition: the groups list their values before the arrange mode")
                .isPositive();

        rail.startArranging();
        assertThat(rail.getGroupOrder())
                .as("The arrange mode keeps all groups in the rail")
                .containsExactlyElementsOf(initialOrder);
        rail.waitForFilterValues(false);
        assertThat(rail.countFilterValues())
                .as("EPBDS-16753: in the arrange mode no group lists its values")
                .isZero();

        dragAndExpect(rail, REPOSITORY_FILTER_GROUP, STATUS_FILTER_GROUP,
                List.of(BRANCH_FILTER_GROUP, STATUS_FILTER_GROUP, REPOSITORY_FILTER_GROUP),
                "The first group dragged by the middle of its header must reach the last place");
        dragAndExpect(rail, REPOSITORY_FILTER_GROUP, BRANCH_FILTER_GROUP, initialOrder,
                "The last group dragged by the middle of its header must reach the first place");
        dragAndExpect(rail, BRANCH_FILTER_GROUP, STATUS_FILTER_GROUP,
                List.of(REPOSITORY_FILTER_GROUP, STATUS_FILTER_GROUP, BRANCH_FILTER_GROUP),
                "A group dragged by the middle of its header must take the place of the group it is dropped on");

        rail.finishArranging();
        assertThat(rail.getGroupOrder())
                .as("The new order of the groups stays after Done")
                .containsExactly(REPOSITORY_FILTER_GROUP, STATUS_FILTER_GROUP, BRANCH_FILTER_GROUP);
        rail.waitForFilterValues(true);
        assertThat(rail.countFilterValues())
                .as("EPBDS-16753: after Done the groups list their values again")
                .isPositive();
    }

    private void dragAndExpect(ProjectsFilterRailComponent rail, String group, String target, List<String> expected,
                               String expectation) {
        rail.dragGroupOnto(group, target);
        WaitUtil.waitForCondition(() -> rail.getGroupOrder().equals(expected), DROPPED_MS, 100,
                "Waiting for the dropped filter group to take its place");
        assertThat(rail.getGroupOrder())
                .as("EPBDS-16753: " + expectation)
                .containsExactlyElementsOf(expected);
    }
}
