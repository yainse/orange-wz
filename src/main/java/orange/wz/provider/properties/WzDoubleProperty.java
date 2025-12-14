package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

@Setter
@Getter
public class WzDoubleProperty extends WzImageProperty {
    private double value;
    private final String type = "double";

    public WzDoubleProperty(String name, double value, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
        this.value = value;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 5);
        writer.putDouble(value);
    }

    @Override
    public WzDoubleProperty deepClone(WzObject parent) {
        return new WzDoubleProperty(name, value, parent, null);
    }
}
