package zk.extra;


import au.org.ala.spatial.StringConstants;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.sys.IdGenerator;

/**
 * @author Angus
 */
public class IdGeneratorImpl implements IdGenerator {
    @Override
    public String nextComponentUuid(Desktop desktop, Component comp, ComponentInfo ci) {
        int i = Integer.parseInt(desktop.getAttribute(StringConstants.ID_NUM).toString());
        i++;
        desktop.setAttribute(StringConstants.ID_NUM, String.valueOf(i));
        return "zkcomp" + i;
    }

    @Override
    public String nextDesktopId(Desktop desktop) {
        if (desktop.getAttribute(StringConstants.ID_NUM) == null) {
            String number = "0";
            desktop.setAttribute(StringConstants.ID_NUM, number);
        }
        return null;
    }

    @Override
    public String nextPageUuid(Page page) {
        return null;
    }

}