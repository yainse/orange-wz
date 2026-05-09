package orange.wz.gui.component.panel;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzIntProperty;
import orange.wz.provider.properties.WzListProperty;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class EditPaneDetachedSubtreeTest {
    @Test
    void detachSubtreeWithoutReleaseShouldKeepUserObjectsAndChildren() throws Exception {
        EditPane source = headlessEditPane();
        WzListProperty parent = list("parent");
        WzIntProperty child = intProp("child");
        parent.addChild(child);

        DefaultMutableTreeNode sourceNode = source.insertNodeToTree(source.getTreeRoot(), parent, true);
        DefaultMutableTreeNode childNode = source.insertNodeToTree(sourceNode, child, false);

        source.detachSubtreeWithoutRelease(sourceNode);

        assertNull(sourceNode.getParent());
        assertSame(parent, sourceNode.getUserObject());
        assertEquals(1, sourceNode.getChildCount());
        assertSame(childNode, sourceNode.getChildAt(0));
        assertSame(child, childNode.getUserObject());
    }

    @Test
    void insertDetachedSubtreeShouldMoveExistingNodeToTargetPane() throws Exception {
        EditPane source = headlessEditPane();
        EditPane target = headlessEditPane();
        WzListProperty parent = list("parent");
        WzIntProperty child = intProp("child");
        parent.addChild(child);

        DefaultMutableTreeNode sourceNode = source.insertNodeToTree(source.getTreeRoot(), parent, true);
        DefaultMutableTreeNode childNode = source.insertNodeToTree(sourceNode, child, false);

        source.detachSubtreeWithoutRelease(sourceNode);
        target.insertDetachedSubtree(target.getTreeRoot(), sourceNode, true);

        assertSame(target.getTreeRoot(), sourceNode.getParent());
        assertEquals(1, target.getTreeRoot().getChildCount());
        assertSame(sourceNode, target.getTreeRoot().getChildAt(0));
        assertSame(parent, sourceNode.getUserObject());
        assertEquals(1, sourceNode.getChildCount());
        assertSame(childNode, sourceNode.getChildAt(0));
        assertSame(child, childNode.getUserObject());
    }

    @Test
    void insertDetachedSubtreeShouldIgnoreNonDetachedNode() throws Exception {
        EditPane source = headlessEditPane();
        EditPane target = headlessEditPane();
        WzListProperty parent = list("parent");

        DefaultMutableTreeNode sourceNode = source.insertNodeToTree(source.getTreeRoot(), parent, true);

        target.insertDetachedSubtree(target.getTreeRoot(), sourceNode, true);

        assertSame(source.getTreeRoot(), sourceNode.getParent());
        assertEquals(0, target.getTreeRoot().getChildCount());
    }

    @Test
    void removeNodeFromTreeShouldClearRemovedNodeUserObjects() throws Exception {
        EditPane source = headlessEditPane();
        WzListProperty parent = list("parent");
        WzIntProperty child = intProp("child");
        parent.addChild(child);

        DefaultMutableTreeNode sourceNode = source.insertNodeToTree(source.getTreeRoot(), parent, true);
        DefaultMutableTreeNode childNode = source.insertNodeToTree(sourceNode, child, false);

        source.removeNodeFromTree(sourceNode);

        assertNull(sourceNode.getUserObject());
        assertNull(childNode.getUserObject());
        assertNull(sourceNode.getParent());
    }

    private static EditPane headlessEditPane() throws Exception {
        EditPane editPane = (EditPane) unsafe().allocateInstance(EditPane.class);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        JTree tree = new JTree(root);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        setField(editPane, "treeRoot", root);
        setField(editPane, "tree", tree);
        setField(editPane, "treeModel", model);
        return editPane;
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = EditPane.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static WzListProperty list(String name) {
        return new WzListProperty(name, null, (WzImage) null);
    }

    private static WzIntProperty intProp(String name) {
        return new WzIntProperty(name, 0, null, null);
    }
}
