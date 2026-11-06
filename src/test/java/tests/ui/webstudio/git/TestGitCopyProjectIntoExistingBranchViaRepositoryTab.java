package tests.ui.webstudio.git;

import configuration.annotations.Description;
import configuration.annotations.TestCaseId;
import configuration.annotations.AppContainerConfig;
import configuration.appcontainer.AppContainerStartParameters;
import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.MessageComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.components.common.ThemeMenuComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.ProjectDetailPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.service.LoginService;
import helpers.service.UserService;
import helpers.utils.WaitUtil;
import org.testng.annotations.Test;
import tests.BaseTest;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGitCopyProjectIntoExistingBranchViaRepositoryTab extends BaseTest {

    private static final String PROJECT_NAME = "TestProject";
    private static final String SECOND_PROJECT_NAME = "TestProject2";
    private static final String BRANCH_NAME = "myBranch";
    private static final String EXPECTED_ERROR_MESSAGE = "Branch '" + BRANCH_NAME + "' already exists in repository.";
    private static final Pattern RGB = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*([\\d.]+))?");
    private static final int MIDDLE_BRIGHTNESS = 128;
    private static final int THEME_APPLIED_MS = 5000;

    @Test
    @TestCaseId({"EPBDS-8495", "EPBDS-16651"})
    @Description("Git - Copy project into existing branch via Repository Tab should display error. "
            + "EPBDS-16651: with the dark appearance selected, the pop-up that reports a project copied into a "
            + "new branch is painted dark with light text, not white.")
    @AppContainerConfig(startParams = AppContainerStartParameters.DEFAULT_STUDIO_PARAMS)
    public void testGitCopyProjectIntoExistingBranchRepositoryTab() {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(User.ADMIN));
        ThemeMenuComponent themeMenu = new ThemeMenuComponent();
        themeMenu.setDarkAppearance(true);
        WaitUtil.requireCondition(() -> brightness(themeMenu.getPageBackgroundColor()).orElse(MIDDLE_BRIGHTNESS * 2)
                < MIDDLE_BRIGHTNESS, THEME_APPLIED_MS, 100, "Waiting for the page to be painted in the dark appearance");
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent()
                .selectTab(TabSwitcherComponent.TabName.REPOSITORY);

        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, PROJECT_NAME, "Sample Project");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.TEMPLATE, SECOND_PROJECT_NAME, "Sample Project");

        ProjectDetailPage projectDetail = repositoryPage.openProjectDetail(PROJECT_NAME);
        assertNoticeIsDark(projectDetail.copyIntoNewBranchReadingNotice(BRANCH_NAME, BRANCH_NAME));
        projectDetail.waitUntilSpinnerLoaded();

        String error = repositoryPage.openProjectsList()
                .openProjectDetail(SECOND_PROJECT_NAME)
                .createBranchExpectingError(BRANCH_NAME);

        assertThat(error)
                .as("Error message about existing branch should be displayed")
                .contains(EXPECTED_ERROR_MESSAGE);
    }

    private static void assertNoticeIsDark(MessageComponent.NoticeColors notice) {
        assertThat(opaqueBrightness(notice.background()))
                .as("The pop-up must take the dark appearance, not stay white (EPBDS-16651), but its background was %s",
                        notice.background())
                .isLessThan(MIDDLE_BRIGHTNESS);
        assertThat(opaqueBrightness(notice.text()))
                .as("The pop-up text must be light on the dark background (EPBDS-16651), but its colour was %s",
                        notice.text())
                .isGreaterThan(MIDDLE_BRIGHTNESS);
    }

    private static int opaqueBrightness(String cssColor) {
        return brightness(cssColor).orElseThrow(() -> new AssertionError("An opaque colour was expected, but it was " + cssColor));
    }

    private static OptionalInt brightness(String cssColor) {
        Matcher matcher = RGB.matcher(cssColor);
        if (!matcher.find() || matcher.group(4) != null && Double.parseDouble(matcher.group(4)) == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((Integer.parseInt(matcher.group(1)) + Integer.parseInt(matcher.group(2))
                + Integer.parseInt(matcher.group(3))) / 3);
    }
}
