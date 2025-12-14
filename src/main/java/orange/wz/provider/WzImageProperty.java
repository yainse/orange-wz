package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.properties.*;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter
public abstract class WzImageProperty extends WzObject {
    protected WzImage wzImage;

    protected WzImageProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, parent);
        this.wzImage = wzImage;
    }

    public static List<WzImageProperty> parsePropertyList(int offset, BinaryReader reader, WzObject parent) {
        WzImage wzImage = null;
        if (parent instanceof WzImage img) {
            wzImage = img;
        } else if (parent instanceof WzImageProperty prop) {
            wzImage = prop.getWzImage();
        }

        int entryCount = reader.readCompressedInt();
        List<WzImageProperty> properties = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            String name = reader.readStringBlock(offset);
            byte pType = reader.getByte();
            switch (pType) {
                case 0: // null
                    properties.add(new WzNullProperty(name, parent, wzImage));
                    break;
                case 2, 11: // short
                    properties.add(new WzShortProperty(name, reader.getShort(), parent, wzImage));
                    break;
                case 3, 19: // int
                    properties.add(new WzIntProperty(name, reader.readCompressedInt(), parent, wzImage));
                    break;
                case 20: // long
                    properties.add(new WzLongProperty(name, reader.readCompressedLong(), parent, wzImage));
                    break;
                case 4: // float
                    properties.add(new WzFloatProperty(name, reader.readCompressedFloat(), parent, wzImage));
                    break;
                case 5: // double
                    properties.add(new WzDoubleProperty(name, reader.getDouble(), parent, wzImage));
                    break;
                case 8: // string
                    properties.add(new WzStringProperty(name, reader.readStringBlock(offset), parent, wzImage));
                    break;
                case 9:
                    int eob = reader.getInt() + reader.getPosition();
                    WzImageProperty exProp = parseExtendedProp(reader, offset, eob, name, parent);
                    properties.add(exProp);
                    if (reader.getPosition() != eob) reader.setPosition(eob);
                    break;
                default:
                    log.error("Unknown property type at ParsePropertyList, pType = {}", pType);
            }
        }
        return properties;
    }

    private static WzExtended parseExtendedProp(BinaryReader reader, int offset, long endOfBlock, String name, WzObject parent) {
        try {
            final byte stringType = reader.getByte();
            return switch (stringType) {
                case 0x01, WzImage.withOffsetFlag ->
                        extractMore(reader, offset, endOfBlock, name, reader.readStringAtOffset(offset + reader.getInt()), parent);
                case 0x00, WzImage.withoutOffsetFlag -> extractMore(reader, offset, endOfBlock, name, "", parent);
                default -> throw new Exception("Invalid byte read at ParseExtendedProp");
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static WzExtended extractMore(BinaryReader reader, int offset, long eob, String name, String iname, WzObject parent) {
        WzImage wzImage = null;
        if (parent instanceof WzImage img) {
            wzImage = img;
        } else if (parent instanceof WzImageProperty prop) {
            wzImage = prop.getWzImage();
        }

        if (iname.equalsIgnoreCase("")) {
            iname = reader.readString();
        }
        final WzPropertyType propertyType = WzPropertyType.getByString(iname);
        try {
            switch (propertyType) {
                case WzPropertyType.LIST: {
                    WzListProperty subProp = new WzListProperty(name, parent, wzImage);
                    reader.skip(2); // Reserved?
                    subProp.addProperties(WzImageProperty.parsePropertyList(offset, reader, subProp));
                    return subProp;
                }
                case WzPropertyType.CANVAS: {
                    WzCanvasProperty canvasProp = new WzCanvasProperty(name, parent, wzImage);
                    reader.skip(1);
                    if (reader.getByte() == 1) {
                        reader.skip(2);
                        canvasProp.addProperties(WzImageProperty.parsePropertyList(offset, reader, canvasProp));
                    }
                    WzPngProperty png = new WzPngProperty(name, canvasProp, wzImage);
                    png.setData(reader);
                    canvasProp.setPng(png);
                    return canvasProp;
                }
                case WzPropertyType.VECTOR: {
                    int x = reader.readCompressedInt();
                    int y = reader.readCompressedInt();
                    return new WzVectorProperty(name, x, y, parent, wzImage);
                }
                case WzPropertyType.CONVEX: {
                    WzConvexProperty convexProp = new WzConvexProperty(name, parent, wzImage);
                    int convexEntryCount = reader.readCompressedInt();
                    for (int i = 0; i < convexEntryCount; i++) {
                        convexProp.addProperty(parseExtendedProp(reader, offset, 0, name, convexProp));
                    }
                    return convexProp;
                }
                case WzPropertyType.SOUND: {
                    WzSoundProperty soundProp = new WzSoundProperty(name, parent, wzImage);
                    soundProp.setData(reader);
                    return soundProp;
                }
                case WzPropertyType.UOL: {
                    reader.skip(1);
                    String value = switch (reader.getByte()) {
                        case 0 -> reader.readString();
                        case 1 -> reader.readStringAtOffset(offset + reader.getInt());
                        default -> throw new Exception("Unsupported UOL type");
                    };

                    return new WzUOLProperty(name, value, parent, wzImage);
                }
                case WzPropertyType.RAW_DATA: {  // GMS v220++
                    // GMS v255+
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\3\skeleton.skel
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\16\skeleton.skel
                    // UI_000.wz\Login.img\RaceSelect_new\Back0\1005\Sia.skel
                    byte type = reader.getByte();
                    WzRawDataProperty rawData = new WzRawDataProperty(name, type, -1, parent, wzImage);

                    // type 0: do nothing
                    // type 1: similar to CanvasProperty that has binary data (the PNG) and sub properties (the origin, _hash, etc.)
                    if (type == 1) {
                        if (reader.getByte() == 1) {
                            reader.skip(2);
                            rawData.getProperties().addAll(parsePropertyList(offset, reader, rawData));
                        }
                    }
                    // all types: parse the binary data (similar to parse WzPngProperty for WzCanvasProperty)
                    int length = reader.readCompressedInt();
                    rawData.setLength(length);
                    rawData.setBytes(reader.getBytes(length));
                    return rawData;
                }
                default: {
                    throw new Exception("Unknown iname: " + iname);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void writeValue(BinaryWriter writer);

    public abstract WzImageProperty deepClone(WzObject parent);
}
