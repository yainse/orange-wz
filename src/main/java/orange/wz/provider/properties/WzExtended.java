package orange.wz.provider.properties;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;

public abstract class WzExtended extends WzImageProperty {
    protected WzExtended(String name, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
    }
}
