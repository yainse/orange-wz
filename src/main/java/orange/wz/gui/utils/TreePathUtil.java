package orange.wz.gui.utils;

import javax.swing.tree.TreePath;

public final class TreePathUtil {
    public static boolean isNullOrMultiple(TreePath[] treePaths) {
        if (treePaths == null) return true;
        if (treePaths.length != 1) {
            JMessageUtil.error("该功能不支持多选");
            return true;
        }

        return false;
    }
}
