package zk.extra;

import java.net.URL;
import java.util.Locale;

import org.ala.spatial.util.CommonData;

/**
 * 
 * Locates the label information in the biocache i18n webservice.
 * 
 * @author Natasha Carter (natasha.carter@csiro.au)
 *
 */
public class BiocacheLabelLocator implements org.zkoss.util.resource.LabelLocator {

    @Override
    public URL locate(Locale locale) throws Exception {
        // TODO Auto-generated method stub
        String suffix = locale != null || isEnglish(locale) ?"":locale +".properties";
        String url = CommonData.biocacheServer+"/facets/i18n"+suffix;
        System.out.println("Retrieve BiocacheLocationURL for " + locale +" " + url);
        return new URL(url);
    }
    
    private boolean isEnglish(Locale locale){
        return locale == null || locale.getLanguage().equals(Locale.ENGLISH.getLanguage());
    }

}
