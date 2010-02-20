package au.org.emii.portal;

import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;
import au.org.emii.portal.mest.MestConfiguration;
import java.util.List;

public class SearchCatalogueImpl extends AbstractIdentifierImpl implements SearchCatalogue {

    private static final long serialVersionUID = 1L;
    private String uri = null;
    private String version = null;
    private String username = null;
    private String password = null;
    private MestConfiguration mestConfiguration = null;

    
    /**
     * List of search terms that are findable in this mest instance
     * obtained by asking the MEST for a list of search terms
     */
    private List<String> searchTerms = null;

    @Override
    public String getUri() {
        return uri;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @LogSetterValue
    @CheckNotNull
    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public List<String> getSearchKeywords() {
        return searchTerms;
    }

    @CheckNotNull
    @Override
    public void setSearchKeywords(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }

    @LogSetterValue
    @CheckNotNull
    private String serviceUri(String path) {
        return uri + path;
    }

    @Override
    public MestConfiguration getMestConfiguration() {
        return mestConfiguration;
    }

    @CheckNotNull
    @Override
    public void setMestConfiguration(MestConfiguration mestConfiguration) {
        this.mestConfiguration = mestConfiguration;
    }

    @Override
    public String searchKeywordsUri() {
        return uri + mestConfiguration.getServicePathKeyword();
    }

}
