package orange.wz.provider.properties;

import orange.wz.provider.BinaryWriter;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Setter
@Getter
@SuperBuilder
public class WzLongProperty extends WzImageProperty {
    private long value;
    private final String type = "long";

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.putByte((byte) 20);
        writer.writeCompressedLong(value);
    }

    @Override
    public WzLongProperty deepClone(WzObject parent) {
        return WzLongProperty.builder().name(getName()).parent(parent).value(value).build();
    }
}
