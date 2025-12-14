package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

@Setter
@Getter
public class WzNullProperty extends WzImageProperty {
    private final String type = "null";

    public WzNullProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 0);
    }

    @Override
    public WzNullProperty deepClone(WzObject parent) {
        return new WzNullProperty(name, parent, null);
    }
}
