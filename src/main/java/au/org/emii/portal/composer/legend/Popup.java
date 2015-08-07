package au.org.emii.portal.composer.legend;

import org.zkoss.zul.Caption;
import org.zkoss.zul.Image;
import org.zkoss.zul.Window;

public class Popup extends Window {

    private static final long serialVersionUID = 1L;

    @Override
    public void doOverlapped() {
        super.doOverlapped();
    }

    @Override
    public void doModal() {
        super.doModal();
    }

    public void init(String name, String imageUrl) {
        ((Image) getFellow("img")).setSrc(imageUrl);
        ((Caption) getFellow("caption")).setLabel(name);
    }
}
