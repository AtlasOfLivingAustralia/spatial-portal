/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.user;

import au.org.emii.portal.user.PortalUser;

/**
 * A user registered for MEST
 * @author geoff
 */
public class PortalUserImpl implements PortalUser {
    private String username = null;
    private String lastName = null;
    private String firstName = null;
    private String address = null;
    private String state = null;
    private String zip = null;
    private String country = null;
    private String email = null;
    private String organisation = null;
    private int type = USER_NOT_LOGGED_IN;


    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }



    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getZip() {
        return zip;
    }

    @Override
    public void setZip(String zip) {
        this.zip = zip;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int type) {
       this.type = type;
    }

    @Override
    public String getOrganisation() {
        return organisation;
    }

    @Override
    public void setOrganisation(String organisation) {
        this.organisation = organisation;
    }

    @Override
    public boolean isLoggedIn() {
        return this.type > USER_NOT_LOGGED_IN;
    }

    @Override
    public boolean isAdmin() {
        return this.type == USER_ADMIN;
    }

        @Override
        public String getState() {
                return state;
        }

        @Override
        public void setState(String state) {
                this.state = state;
        }


}
