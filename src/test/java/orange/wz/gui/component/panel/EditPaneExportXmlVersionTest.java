package orange.wz.gui.component.panel;

import orange.wz.provider.tools.XmlExportVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditPaneExportXmlVersionTest {

    @Test
    void exportXmlShouldExposeVersionFromData() {
        assertEquals(XmlExportVersion.DEFAULT, EditPane.normalizeXmlExportVersion(null));
        assertEquals(XmlExportVersion.DEFAULT, EditPane.normalizeXmlExportVersion(XmlExportVersion.DEFAULT));
        assertEquals(XmlExportVersion.V125, EditPane.normalizeXmlExportVersion(XmlExportVersion.V125));
    }
}
