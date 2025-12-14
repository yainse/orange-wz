package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

@Setter
@Getter
public class WzIntProperty extends WzImageProperty {
    private int value;
    private final String type = "int";

    public WzIntProperty(String name, int value, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 3);
        writer.writeCompressedInt(value);
    }

    @Override
    public WzIntProperty deepClone(WzObject parent) {
        return new WzIntProperty(name, value, parent, null);
    }
}
