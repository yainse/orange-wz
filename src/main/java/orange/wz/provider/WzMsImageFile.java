package orange.wz.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import orange.wz.provider.ms.WzMsFile;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.tools.BinaryReader;
import orange.wz.provider.tools.WzFileStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class WzMsImageFile extends WzImageFile {
    private List<WzMsFile.EntryImage> sourceEntries = new ArrayList<>();

    public WzMsImageFile(String name, String filePath, String keyBoxName, byte[] iv, byte[] key) {
        super(name, filePath, keyBoxName, iv, key);
    }

    @Override
    public synchronized boolean parse(boolean realParse) {
        if (getStatus() == WzFileStatus.PARSE_SUCCESS) {
            return true;
        }
        if (!realParse) {
            return true;
        }
        try {
            List<WzMsFile.EntryImage> loaded = WzMsFile.load(Path.of(getFilePath()), getIv(), getKey());
            sourceEntries = loaded;
            super.setReader(new BinaryReader(getIv(), getKey()));
            unparse();
            for (WzMsFile.EntryImage entry : loaded) {
                String nodeName = entry.getEntryName();
                int slash = nodeName.lastIndexOf('/');
                if (slash >= 0) {
                    nodeName = nodeName.substring(slash + 1);
                }
                WzListProperty wrapper = new WzListProperty(nodeName, this, this);
                for (WzImageProperty child : entry.getImage().getChildren()) {
                    wrapper.addChild(child.deepClone(wrapper), true);
                }
                wrapper.setChildrenWzImage(this);
                addChild(wrapper, true);
            }
            setStatus(WzFileStatus.PARSE_SUCCESS);
            setChanged(false);
            return true;
        } catch (Exception e) {
            log.error("MS 文件解析失败 {}: {}", getName(), e.getMessage(), e);
            setStatus(WzFileStatus.ERROR_SPECIAL_ENCODE);
            return false;
        }
    }

    @Override
    public boolean save() {
        log.warn(".ms save is not supported in read-only mode: {}", getName());
        return false;
    }

    public boolean saveAsWz(Path outPath) {
        throw new UnsupportedOperationException(".ms save/export-to-wz is not supported in read-only mode");
    }
}
