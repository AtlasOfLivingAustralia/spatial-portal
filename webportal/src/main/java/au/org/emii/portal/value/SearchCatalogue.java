/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.value;

import au.org.emii.portal.aspect.CheckNotNull;
import au.org.emii.portal.aspect.LogSetterValue;
import au.org.emii.portal.mest.MestConfiguration;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author geoff
 */
public interface SearchCatalogue extends AbstractIdentifier, Serializable {

    public MestConfiguration getMestConfiguration();

    public String getPassword();

    public List<String> getSearchKeywords();

    public String getUri();

    public String getUsername();

    public String getVersion();

    @CheckNotNull
    public void setMestConfiguration(MestConfiguration mestConfiguration);

    @LogSetterValue
    public void setPassword(String password);

    public void setSearchKeywords(List<String> searchKeywords);

    @LogSetterValue
    public void setUri(String uri);

    @LogSetterValue
    public void setUsername(String username);

    @LogSetterValue
    public void setVersion(String version);

    public String searchKeywordsUri();

    public Object clone() throws CloneNotSupportedException;
}
