/**
 * ************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * *************************************************************************
 */
package org.ala.spatial.authentication;

/**
 * Helper class for getting/setting user management
 * and session management data
 *
 * @author ajayr
 */
public interface UserService {

    /**
     * Get a particular session data.
     *
     * @param sid Key for the session param id
     * @return Object value for the requested session param id
     */
    public Object getSessionInfo(String sid);

    /**
     * Set a particular session data.
     *
     * @param sid Key for the session param id
     * @param obj Value for the session param id
     */
    public void setSessionInfo(String sid, Object obj);
}
