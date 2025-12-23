package orange.wz.gui.component.form.data;

import lombok.Getter;

@Getter
public final class ExportXmlData {
    private final int indent;
    private final boolean exportMedia;
    private final String exportPath;

    public ExportXmlData(int indent, boolean exportMedia, String exportPath) {
        this.indent = indent;
        this.exportMedia = exportMedia;
        this.exportPath = exportPath;
    }
}
