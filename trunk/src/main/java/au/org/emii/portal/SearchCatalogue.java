package au.org.emii.portal;

import au.org.emii.portal.webservice.UserManagementWebService;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCatalogue extends AbstractIdentifier implements UserManagementWebService {

    private static final long serialVersionUID = 1L;
    private String uri = null;
    private String protocol = null;
    private String version = null;
    private String username = null;
    private String password = null;
    private String selfRegistrationServicePath = null;
    private String loginServicePath = null;
    private String userInfoServicePath = null;
    private String newUserProfile = null;
    private String administratorProfile = null;
    private String resetPasswordServicePath = null;
    private String changePasswordServicePath = null;
    
    /**
     * List of search terms that are findable in this mest instance
     * obtained by asking the MEST for a list of search terms
     */
    private List<String> searchTerms = null;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAdministratorProfile() {
        return administratorProfile;
    }

    public void setAdministratorProfile(String administratorProfile) {
        this.administratorProfile = administratorProfile;
    }

    public String getLoginServicePath() {
        return loginServicePath;
    }

    public void setLoginServicePath(String loginServicePath) {
        this.loginServicePath = loginServicePath;
    }

    public String getNewUserProfile() {
        return newUserProfile;
    }

    public void setNewUserProfile(String newUserProfile) {
        this.newUserProfile = newUserProfile;
    }

    public String getSelfRegistrationServicePath() {
        return selfRegistrationServicePath;
    }

    public void setSelfRegistrationServicePath(String selfRegistrationServicePath) {
        this.selfRegistrationServicePath = selfRegistrationServicePath;
    }

    public String getUserInfoServicePath() {
        return userInfoServicePath;
    }

    public void setUserInfoServicePath(String userInfoServicePath) {
        this.userInfoServicePath = userInfoServicePath;
    }

    public String getChangePasswordServicePath() {
        return changePasswordServicePath;
    }

    public void setChangePasswordServicePath(String changePasswordServicePath) {
        this.changePasswordServicePath = changePasswordServicePath;
    }

    public String getResetPasswordServicePath() {
        return resetPasswordServicePath;
    }

    public void setResetPasswordServicePath(String resetPasswordServicePath) {
        this.resetPasswordServicePath = resetPasswordServicePath;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void copyFrom(au.org.emii.portal.config.xmlbeans.SearchCatalogue sc) {
        setId(sc.getId());
        setName(sc.getName());
        setDescription(sc.getDescription());
        setUri(sc.getUri());
        setProtocol(sc.getProtocol());
        setVersion(sc.getVersion());
        setUsername(sc.getUsername());
        setPassword(sc.getPassword());

        // process the userManagementWebService element if there is one
        au.org.emii.portal.config.xmlbeans.UserManagementWebService umws = sc.getUserManagementWebService();
        if (umws != null) {
            setLoginServicePath(umws.getLoginServicePath());
            setUserInfoServicePath(umws.getUserInfoServicePath());
            setSelfRegistrationServicePath(umws.getSelfRegistrationServicePath());
            setChangePasswordServicePath(umws.getChangePasswordServicePath());
            setResetPasswordServicePath(umws.getResetPasswordServicePath());
            setAdministratorProfile(umws.getAdministratorProfile());
            setNewUserProfile(umws.getNewUserProfile());
        }
    }

    /**
     * URI for the search keywords web service
     * @return
     */
    public String searchTermUri() {
        return serviceUri("mest_keyword_path");
    }

    /**
     * URI for the login service
     * @return
     */
    @Override
    public String loginServiceUri() {
        return serviceUri(loginServicePath);
    }

    /**
     * URI for user info service
     * @return
     */
    @Override
    public String userInfoServiceUri() {
        return serviceUri(userInfoServicePath);
    }

    /**
     * URI for self registration service
     * @return
     */
    @Override
    public String selfRegistrationServiceUri() {
        return serviceUri(selfRegistrationServicePath);
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }

    private String serviceUri(String path) {
        return protocol + "://"
                + uri
                + path;
    }

    @Override
    public String resetPasswordServiceUri() {
        return serviceUri(resetPasswordServicePath);
    }

    @Override
    public String changePasswordServiceUri() {
       return serviceUri(changePasswordServicePath);
    }

    @Override
    public String newUserProfile() {
        return newUserProfile;
    }

    @Override
    public String administratorProfile() {
        return administratorProfile;
    }

    /**
     * Getting the port used to connect to a uri - assume port 80 if nothing
     * specified.  This implementation is made static so that the mocks can
     * use it
     * @param uri
     * @return
     */
    public static int resolveServicePort(String uri) {
        int port;
        String regexp = "\\D*:(\\d+).*";
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(uri);
        if (m.matches()) {
            port = Integer.parseInt(m.group(1));
        } else {
            // port 80 unless otherwise specified
            port = 80;
        }

        return port;
    }

    @Override
    public int servicePort(String uri) {
        return resolveServicePort(uri);
    }
}
