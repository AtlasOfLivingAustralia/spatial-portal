/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.userdata;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.mest.SearchQuery;
import java.util.List;
import org.hibernate.SessionFactory;

/**
 *
 * @author brendon
 */
public interface UserDataDao {

    public void setSessionFactory(SessionFactory usersSessionFactory);

    public UserPortal getCurrentUser();

    public void setCurrentUser(UserPortal currentUser);

    public void fetchUser(String userName);

    public UserMap getUserMapByName(String mapName);

    public UserMap fetchUserMapByName(String mapName);

    public List<UserMap> getUserMaps();

    public List<UserSearch> getUserSearches();

    public UserSearch getUserSearchByName(String searchname);

    public List<MapLayer> getActiveLayers(long mapId);

    public boolean checkMapName(String mapname);

    public boolean checkSearchName(String searchname);

    public void saveMap(String mapname);

    public void saveObjectToDB(Object obj);

    public void deleteObjectFromDb(Object obj);

    public void updateObjectToDB(Object obj);

    public UserMap parsePortalSession();

    public void updateMap(String mapname);

    public void deleteMap(String mapname);

    public void saveSearch(String searchname, SearchQuery sq);

    public MapComposer getMapComposer();

    public void deleteSearch(String searchName);

}
