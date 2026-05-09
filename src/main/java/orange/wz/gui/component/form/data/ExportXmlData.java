package orange.wz.gui.component.form.data;

import lombok.Getter;
import orange.wz.provider.tools.MediaExportType;
import orange.wz.provider.tools.XmlExportVersion;

@Getter
public final class ExportXmlData {
    private final int indent;
    private final MediaExportType meType;
    private final String exportPath;
    private final boolean linux;
    private final XmlExportVersion version;

    public ExportXmlData(int indent, MediaExportType meType, String exportPath, boolean linux) {
        this(indent, meType, exportPath, linux, XmlExportVersion.DEFAULT);
    }

    public ExportXmlData(int indent, MediaExportType meType, String exportPath, boolean linux, XmlExportVersion version) {
        this.indent = indent;
        this.meType = meType;
        this.exportPath = exportPath;
        this.linux = linux;
        this.version = version == null ? XmlExportVersion.DEFAULT : version;
    }
}
