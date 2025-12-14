package orange.wz.provider.properties;

import lombok.Getter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WzConvexProperty extends WzExtended {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private final String type = "convex";

    public WzConvexProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
    }

    public void addProperty(WzImageProperty property) {
        properties.add(property);
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        List<WzExtended> extendedProps = new ArrayList<>();
        for (WzImageProperty prop : properties) {
            if (prop instanceof WzExtended) extendedProps.add((WzExtended) prop);
        }
        writer.writeStringBlock(WzPropertyType.CONVEX.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.writeCompressedInt(extendedProps.size());

        for (WzExtended extendedProp : extendedProps) {
            extendedProp.writeValue(writer);
        }
    }

    @Override
    public WzConvexProperty deepClone(WzObject parent) {
        WzConvexProperty clone = new WzConvexProperty(name, parent, null);
        for (WzImageProperty prop : properties) {
            clone.properties.add(prop.deepClone(clone));
        }
        return clone;
    }
}
