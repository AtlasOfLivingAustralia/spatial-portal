/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.service;

/**
 *
 * @author geoff
 */
public interface ChangePasswordService extends ServiceStatusCodes {

    public int changePassword(String username, String oldPassword, String newPassword);

}
