package helpers.service;

public enum LfsProvider {
    GITHUB("GitHub", null, "GITHUB_TOKEN", "x-access-token"),
    GITLAB("GitLab", "LFS_GITLAB_REPO", "LFS_GITLAB_TOKEN", "openl-lfs-test"),
    BITBUCKET("Bitbucket", "LFS_BITBUCKET_REPO", "LFS_BITBUCKET_TOKEN", "x-bitbucket-api-token-auth");

    private static final String DEFAULT_GITHUB_REPOSITORY = "openl-tablets/openl-tests";

    private final String displayName;
    private final String repositoryVariable;
    private final String tokenVariable;
    private final String login;

    LfsProvider(String displayName, String repositoryVariable, String tokenVariable, String login) {
        this.displayName = displayName;
        this.repositoryVariable = repositoryVariable;
        this.tokenVariable = tokenVariable;
        this.login = login;
    }

    public String displayName() {
        return displayName;
    }

    public String tokenVariable() {
        return tokenVariable;
    }

    public String login() {
        return login;
    }

    public String repositoryUrl() {
        if (repositoryVariable == null) {
            String repository = System.getenv("GITHUB_REPOSITORY");
            return "https://github.com/" + (repository == null || repository.isBlank() ? DEFAULT_GITHUB_REPOSITORY : repository) + ".git";
        }
        String value = System.getenv(repositoryVariable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(displayName + " LFS needs the repository URL in " + repositoryVariable);
        }
        String url = value.trim().replaceFirst("^https://[^@/]*@", "https://").replaceFirst("/+$", "");
        if (!url.startsWith("https://")) {
            throw new IllegalStateException(repositoryVariable + " must be an https URL");
        }
        return url.endsWith(".git") ? url : url + ".git";
    }
}
