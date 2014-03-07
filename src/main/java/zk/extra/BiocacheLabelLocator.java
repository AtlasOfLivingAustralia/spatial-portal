package zk.extra;

import au.org.ala.spatial.util.CommonData;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.Locale;

/**
 * Locates the label information in the biocache i18n webservice.
 *
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
public class BiocacheLabelLocator implements org.zkoss.util.resource.LabelLocator {
    private static Logger logger = Logger.getLogger(BiocacheLabelLocator.class);

    @Override
    public URL locate(Locale locale) throws Exception {
        // TODO Auto-generated method stub
        String suffix = locale != null || isEnglish(locale) ? "" : locale + ".properties";
        String url = CommonData.biocacheServer + "/facets/i18n" + suffix;
        logger.debug("Retrieve BiocacheLocationURL for " + locale + " " + url);
        return new URL(url);
    }

    private boolean isEnglish(Locale locale) {
        return locale == null || locale.getLanguage().equals(Locale.ENGLISH.getLanguage());
    }

}
