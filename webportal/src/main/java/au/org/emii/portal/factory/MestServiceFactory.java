/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.factory;

import au.org.emii.portal.webservice.XmlWebService;
import au.org.emii.portal.mest.webservice.ForgottenPasswordServiceImpl;
import au.org.emii.portal.mest.webservice.LoginServiceImpl;
import au.org.emii.portal.mest.webservice.MestWebService;
import au.org.emii.portal.mest.webservice.MestWebServiceParameters;
import au.org.emii.portal.mest.webservice.MestWebServiceSessionImpl;
import au.org.emii.portal.mest.webservice.RegistrationServiceImpl;
import au.org.emii.portal.mest.webservice.UserInfoServiceGetImpl;
import au.org.emii.portal.service.ForgottenPasswordService;
import au.org.emii.portal.service.LoginService;
import au.org.emii.portal.service.RegistrationService;
import au.org.emii.portal.service.ServiceFactory;
import au.org.emii.portal.service.UserInfoService;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;


/**
 *
 * @author geoff
 */
public abstract class MestServiceFactory implements ServiceFactory {

    private Map<String, MestWebServiceParameters> services = null;
    private Map<String, MestWebServiceSessionImpl> sessions = new HashMap<String, MestWebServiceSessionImpl>();
    private Logger logger = Logger.getLogger(getClass());


    private void setupParameters(MestWebService service, String id) {
        service.setParameters(services.get(id));
        service.setXmlWebService(createXmlWebService());
    }

    @Override
    public LoginService createPortalUserLoginService(String id) {
        return createPortalUserLoginService(id, true);
    }

    
    public LoginService createPortalUserLoginService(String id, boolean createUserInfoService) {
        LoginServiceImpl service = new LoginServiceImpl();
        setupParameters(service, id);
        if (createUserInfoService) {
            service.setUserInfoService(createUserInfoService(id));
        }
        return service;
    }

    @Override
    public LoginService createAdminConsoleLoginService(String id) {
        return createPortalUserLoginService(id);
    }


    @Override
    public UserInfoService createUserInfoService(String id) {
        UserInfoServiceGetImpl service = new UserInfoServiceGetImpl();
        setupParameters(service, id);

        // by convention - use the same mest we are logging into to maintain our
        // session
        service.setWebServiceSession(
                createMestWebServiceSession(id, createPortalUserLoginService(id, false)));

        return service;
    }


    @Override
    public ForgottenPasswordService createForgottenPasswordService(String id) {
        ForgottenPasswordServiceImpl service = new ForgottenPasswordServiceImpl();
        setupParameters(service, id);
        return service;
    }


    @Override
    public RegistrationService createRegistrationService(String id) {
        RegistrationServiceImpl service = new RegistrationServiceImpl();
        setupParameters(service, id);
        return service;
    }

    public Map<String, MestWebServiceParameters> getServices() {
        return services;
    }

    public void setServices(Map<String, MestWebServiceParameters> services) {
        this.services = services;
    }

    /**
     * FIXME !!! URGENT!! this needs to be made long lived to preven killing mest
     * remember its being stored in prototype beans at moment --- RELEASE BLOCKER
     * @param id
     * @return
     */
    public MestWebServiceSessionImpl createMestWebServiceSession(String id, LoginService loginService) {
        MestWebServiceSessionImpl session = sessions.get(id);
        if (session == null) {
            session = new MestWebServiceSessionImpl();
            session.setLoginService(loginService);
            session.setParameters(services.get(id));

            logger.info("created persistent MEST session with key " + id);
            sessions.put(id, session);
        }

        return session;
    }

    /**
     * Spring prototype bean, done by method injection
     */
    public abstract XmlWebService createXmlWebService();

}
