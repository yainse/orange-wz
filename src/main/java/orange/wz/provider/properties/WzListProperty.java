package orange.wz.provider.properties;

import lombok.Getter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryWriter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class WzListProperty extends WzExtended {
    private final List<WzImageProperty> properties = new ArrayList<>();
    private final String type = "list";

    public WzListProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, parent, wzImage);
    }

    public WzListProperty(List<WzImageProperty> properties) {
        super(null, null, null);
        addProperties(properties);
    }

    public void addProperties(List<WzImageProperty> properties) {
        this.properties.addAll(properties);
    }

    public WzImageProperty get(String name) {
        for (WzImageProperty property : properties) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzPropertyType.LIST.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        WzImage.writeListValue(writer, properties);
    }

    @Override
    public WzListProperty deepClone(WzObject parent) {
        WzListProperty clone = new WzListProperty(name, parent, null);
        for (WzImageProperty property : properties) {
            clone.properties.add(property.deepClone(clone));
        }
        return clone;
    }
}
