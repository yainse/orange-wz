package orange.wz.provider.properties;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryReader;

final class TestImage extends WzImage {
    TestImage(BinaryReader reader) {
        super("test.img", (WzObject) null, reader);
        setChanged(false);
    }
}
