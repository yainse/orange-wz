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
public class WzStringProperty extends WzImageProperty {
    private String value;
    private final String type = "string";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 8);
        writer.writeStringBlock(value, 0x00, 0x01);
    }

    @Override
    public WzStringProperty deepClone(WzObject parent) {
        return WzStringProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
