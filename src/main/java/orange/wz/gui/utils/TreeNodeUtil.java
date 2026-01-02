package orange.wz.gui.utils;

import orange.wz.provider.WzObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;

public final class TreeNodeUtil {
    public static List<String> getNodePathWithoutRoot(DefaultMutableTreeNode node) {
        TreeNode[] nodePaths = node.getPath();
        List<String> paths = new ArrayList<>();

        for (int i = 1; i < nodePaths.length; i++) { // 跳过 root
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodePaths[i];
            paths.add(((WzObject) n.getUserObject()).getName());
        }
        return paths;
    }
}
