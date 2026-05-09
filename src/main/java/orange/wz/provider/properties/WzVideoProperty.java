package orange.wz.provider.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.BinaryWriter;
import orange.wz.provider.tools.WzType;

import java.util.List;

/**
 * 对应 Canvas#Video（KMST v1181+）。
 *
 * <p>Linux CLI 第一阶段只做 parse-only：记录视频类型、长度和二进制偏移，避免按 PNG Canvas 解析导致失败。</p>
 */
@Setter
@Getter
public class WzVideoProperty extends WzExtended {
    private byte videoType;
    private int length;
    private int offset;
    @Getter(AccessLevel.NONE)
    private byte[] bytes;

    public WzVideoProperty(String name, WzObject parent, WzImage wzImage) {
        super(name, WzType.VIDEO_PROPERTY, parent, wzImage);
    }

    public void parse(BinaryReader reader, boolean parseNow) {
        videoType = reader.getByte();
        length = reader.readCompressedInt();
        offset = reader.getPosition();
        if (parseNow) {
            getBytes(true);
        } else {
            reader.skip(length);
        }
    }

    public byte[] getBytes(boolean saveInMem) {
        if (bytes != null) {
            return bytes;
        }
        if (wzImage == null || wzImage.getReader() == null) {
            throw new IllegalStateException("Canvas#Video bytes are not available without an attached WZ reader");
        }
        BinaryReader reader = wzImage.getReader();
        int curOffset = reader.getPosition();
        reader.setPosition(offset);
        byte[] returnBytes = reader.getBytes(length);
        reader.setPosition(curOffset);
        if (saveInMem) {
            bytes = returnBytes;
        }
        return returnBytes;
    }

    @Override
    public void writeValue(BinaryWriter writer) {
        writer.writeStringBlock(WzExtendedType.CANVAS_VIDEO.getString(), WzImage.withoutOffsetFlag, WzImage.withOffsetFlag);
        writer.putByte((byte) 0);
        List<WzImageProperty> properties = children.get();
        if (!properties.isEmpty()) {
            writer.putByte((byte) 1);
            WzImage.writeListValue(writer, properties);
        } else {
            writer.putByte((byte) 0);
        }
        writer.putByte(videoType);
        byte[] data = getBytes(false);
        writer.writeCompressedInt(data.length);
        writer.putBytes(data);
    }

    @Override
    public WzVideoProperty deepClone(WzObject parent) {
        WzVideoProperty clone = new WzVideoProperty(name, parent, null);
        clone.videoType = videoType;
        byte[] data = getBytes(false);
        clone.length = data.length;
        clone.offset = 0;
        clone.bytes = new byte[data.length];
        System.arraycopy(data, 0, clone.bytes, 0, data.length);
        for (WzImageProperty property : children.get()) {
            clone.addChild(property.deepClone(clone));
        }
        return clone;
    }
}
