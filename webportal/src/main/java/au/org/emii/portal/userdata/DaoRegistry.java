/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.userdata;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.hibernate.SessionFactory;

public class DaoRegistry {
    private static ApplicationContext ctx;

    static {
        ctx = new ClassPathXmlApplicationContext("usersDataConfig.xml");
    }

    /**
     * Private to make this a singleton.
     */
    private DaoRegistry(){

    }

    public static SessionFactory getSessionFactory(){
        return (SessionFactory) ctx.getBean("factory", SessionFactory.class);
    }

    public static UserDataDao getUserDataDao(){
        return (UserDataDao)ctx.getBean("userDataDao", UserDataDao.class);
    }
}