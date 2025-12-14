package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

@Setter
@Getter
public class WzVectorProperty extends WzExtended {
    private int x;
    private int y;
    private final String type = "vector";

    public WzVectorProperty(String name, int x, int y, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
        this.x = x;
        this.y = y;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.VECTOR.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.writeCompressedInt(x);
        writer.writeCompressedInt(y);
    }

    @Override
    public WzVectorProperty deepClone(WzObject parent) {
        return new WzVectorProperty(name, x, y, parent, null);
    }
}
