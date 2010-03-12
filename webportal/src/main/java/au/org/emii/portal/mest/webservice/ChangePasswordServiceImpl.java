/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.mest.webservice;

import au.org.emii.portal.service.ChangePasswordService;

/**
 *
 * @author geoff
 */
public class ChangePasswordServiceImpl extends MestWebService implements ChangePasswordService {

    @Override
    public int changePassword(String username, String oldPassword, String newPassword) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
