package orange.wz.provider.properties;

import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
public class WzIntProperty extends WzImageProperty {
    private int value;
    private final String type = "int";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 3);
        writer.writeCompressedInt(value);
    }

    @Override
    public WzIntProperty deepClone(WzObject parent) {
        return WzIntProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
