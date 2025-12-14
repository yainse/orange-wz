package orange.wz.provider.properties;

import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class WzRawDataProperty extends WzExtended {
    private byte dataType;
    private int length;
    private byte[] bytes;
    private final List<WzImageProperty> properties = new ArrayList<>();

    public WzRawDataProperty(String name, byte dataType, int length, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
        this.dataType = dataType;
        this.length = length;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.RAW_DATA.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte(dataType);
        if (dataType == 1) {
            if (!properties.isEmpty()) {
                writer.putByte((byte) 1);
                WzImage.writeListValue(writer, properties);
            } else {
                writer.putByte((byte) 0);
            }
        }
        writer.writeCompressedInt(bytes.length);
        writer.putBytes(bytes);
    }

    @Override
    public WzRawDataProperty deepClone(WzObject parent) {
        WzRawDataProperty clone = new WzRawDataProperty(name, dataType, length, parent, null);
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        int len = bytes.length;
        clone.bytes = new byte[len];
        System.arraycopy(bytes, 0, clone.bytes, 0, len);
        return clone;
    }
}
