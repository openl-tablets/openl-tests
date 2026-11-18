package helpers.service;

import configuration.driver.DriverPool;
import domain.serviceclasses.constants.User;
import domain.ui.webstudio.components.admincomponents.SystemSettingsPageComponent;
import domain.ui.webstudio.components.common.CreateNewProjectComponent;
import domain.ui.webstudio.components.common.TabSwitcherComponent;
import domain.ui.webstudio.pages.mainpages.EditorPage;
import domain.ui.webstudio.pages.mainpages.RepositoryPage;
import helpers.utils.StringUtil;

public class WorkflowService {
    private static String loginCreateProject(User user, CreateNewProjectComponent.TabName projectType, String sourceName) {
        LoginService loginService = new LoginService(DriverPool.getPage());
        EditorPage editorPage = loginService.login(UserService.getUser(user));
        RepositoryPage repositoryPage = editorPage.getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        String projectName = StringUtil.generateUniqueName("project");
        repositoryPage.createProject(projectType, projectName, sourceName);
        return projectName;
    }

    public static String createProjectFromExcelFile(String excelFileName) {
        RepositoryPage repositoryPage = new EditorPage().getTabSwitcherComponent().selectTab(TabSwitcherComponent.TabName.REPOSITORY);
        String projectName = StringUtil.generateUniqueName("project");
        repositoryPage.createProject(CreateNewProjectComponent.TabName.EXCEL_FILES, projectName, excelFileName);
        return projectName;
    }

    public static String loginCreateProjectFromZip(User user, String zipFileName) {
        return loginCreateProject(user, CreateNewProjectComponent.TabName.ZIP_ARCHIVE, zipFileName);
    }

    public static String loginCreateProjectFromExcelFile(User user, String excelFileName) {
        return loginCreateProject(user, CreateNewProjectComponent.TabName.EXCEL_FILES, excelFileName);
    }

    public static String loginCreateProjectFromTemplate(User user, String templateName) {
        return loginCreateProject(user, CreateNewProjectComponent.TabName.TEMPLATE, templateName);
    }

    public static void setDetectProjectsByExcelFiles(User user, boolean enable) {
        SystemSettingsPageComponent systemSettings = new RepositoryPage().openUserMenu()
                .navigateToAdministration()
                .navigateToSystemSettingsPage();
        systemSettings.setDetectProjectsByExcelFiles(enable);
        systemSettings.applySettingsAndRelogin(user);
    }

    public static String loginCreateProjectWithoutDescriptor(User user, String templateName) {
        String projectName = loginCreateProjectFromTemplate(user, templateName);
        setDetectProjectsByExcelFiles(user, true);
        new RepositoryPage().openProjectsList().openProjectDetail(projectName).deleteFile("rules.xml");
        new RepositoryPage().openProjectsList().saveProject(projectName, "Descriptor deleted");
        setDetectProjectsByExcelFiles(user, false);
        return projectName;
    }
}