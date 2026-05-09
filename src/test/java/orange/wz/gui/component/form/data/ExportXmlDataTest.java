package orange.wz.gui.component.form.data;

import orange.wz.provider.tools.MediaExportType;
import orange.wz.provider.tools.XmlExportVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportXmlDataTest {

    @Test
    void constructorShouldCarryProviderXmlExportVersion() {
        ExportXmlData data = new ExportXmlData(2, MediaExportType.FILE, "/tmp/out", true, XmlExportVersion.V125);

        assertEquals(2, data.getIndent());
        assertEquals(MediaExportType.FILE, data.getMeType());
        assertEquals("/tmp/out", data.getExportPath());
        assertEquals(XmlExportVersion.V125, data.getVersion());
    }

    @Test
    void legacyConstructorShouldDefaultXmlExportVersion() {
        ExportXmlData data = new ExportXmlData(4, MediaExportType.NONE, "/tmp/out", false);

        assertEquals(XmlExportVersion.DEFAULT, data.getVersion());
    }
}
