/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.org.emii.portal.config;

import au.org.emii.portal.lang.LanguagePack;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author geoff
 */
public class PropertiesWriterImpl implements PropertiesWriter {

    private Logger logger = Logger.getLogger(getClass());
    @Autowired
    private LanguagePack languagePack = null;

    public LanguagePack getLanguagePack() {
        return languagePack;
    }

    public void setLanguagePack(LanguagePack languagePack) {
        this.languagePack = languagePack;
    }

    @Override
    public boolean write(String filename, Properties props, String portalUsername) {
        boolean saved = false;
        try {
            props.store(
                    new FileOutputStream(
                        filename),
                        languagePack.getCompoundLang(
                            "properties_file_update_comment", new Object[] {portalUsername})
            );
            saved = true;

        } catch (IOException ex) {
            logger.error("error saving properties file '" + filename + "': " + ex.getMessage());
        }
        return saved;
    }

}
