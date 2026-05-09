package orange.wz.gui.component.panel;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzIntProperty;
import orange.wz.provider.properties.WzListProperty;
import orange.wz.provider.tools.BinaryReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditPaneBatchDeleteParityTest {

    @Test
    void listParityDirectChildNamesShouldReturnNumericNamesByParity() {
        WzListProperty root = list("root", null,
                intProp("0"), intProp("1"), intProp("2"), intProp("3"), intProp("10"),
                intProp("01"), intProp("-1"), intProp("foo"));

        assertEquals(List.of("1", "3"), EditPane.listParityDirectChildNames(root, true, false));
        assertEquals(List.of("0", "2", "10"), EditPane.listParityDirectChildNames(root, false, true));
        assertEquals(List.of("0", "1", "2", "3", "10"), EditPane.listParityDirectChildNames(root, true, true));
        assertEquals(List.of(), EditPane.listParityDirectChildNames(root, false, false));
    }

    @Test
    void deleteParityDirectChildrenShouldOnlyDeleteDirectNumericNames() {
        TestImage image = new TestImage(true);
        WzListProperty nested = list("nested", image, intProp("4", image));
        WzListProperty root = list("root", image, intProp("0", image), intProp("1", image), intProp("2", image), intProp("foo", image), nested);

        EditPane.ParityBatchDeleteResult result = EditPane.deleteParityDirectChildren(root, true, false);

        assertEquals(1, result.success());
        assertEquals(0, result.failed());
        assertTrue(result.refreshTree());
        assertEquals(List.of("0", "2", "foo", "nested"), childNames(root));
        assertEquals(List.of("4"), childNames(nested));
    }

    @Test
    void deleteParityDirectChildrenShouldReportFailureWhenListPropertyHasNoImage() {
        WzListProperty root = list("root", null, intProp("1"), intProp("2"));

        EditPane.ParityBatchDeleteResult result = EditPane.deleteParityDirectChildren(root, true, false);

        assertEquals(0, result.success());
        assertEquals(1, result.failed());
        assertFalse(result.refreshTree());
        assertEquals(List.of("1", "2"), childNames(root));
    }

    @Test
    void deleteParityDirectChildrenShouldReportFailureWhenImageCannotParse() {
        TestImage image = new TestImage(false);

        EditPane.ParityBatchDeleteResult result = EditPane.deleteParityDirectChildren(image, true, false);

        assertEquals(0, result.success());
        assertEquals(1, result.failed());
        assertFalse(result.refreshTree());
    }

    @Test
    void matchesNumericParityShouldIgnoreSignedOrPaddedNames() {
        assertTrue(EditPane.matchesNumericParity("1", true, false));
        assertTrue(EditPane.matchesNumericParity("2", false, true));
        assertFalse(EditPane.matchesNumericParity("01", true, true));
        assertFalse(EditPane.matchesNumericParity("-1", true, true));
        assertFalse(EditPane.matchesNumericParity("foo", true, true));
        assertFalse(EditPane.matchesNumericParity("", true, true));
    }

    private static WzListProperty list(String name, WzImage image, WzImageProperty... children) {
        WzListProperty list = new WzListProperty(name, image, image);
        for (WzImageProperty child : children) {
            list.addChild(child);
        }
        return list;
    }

    private static WzIntProperty intProp(String name, WzImage image) {
        return new WzIntProperty(name, 0, image, image);
    }

    private static WzIntProperty intProp(String name) {
        return intProp(name, null);
    }

    private static List<String> childNames(WzImageProperty property) {
        return property.getChildren().stream().map(WzImageProperty::getName).toList();
    }

    private static final class TestImage extends WzImage {
        private final boolean parseResult;

        TestImage(boolean parseResult) {
            super("test.img", (WzObject) null, (BinaryReader) null);
            this.parseResult = parseResult;
            setChanged(false);
        }

        @Override
        public synchronized boolean parse(boolean realParse) {
            return parseResult;
        }
    }
}
