package orange.wz.gui.utils;

import orange.wz.provider.WzImage;
import orange.wz.provider.WzImageProperty;
import orange.wz.provider.WzObject;
import orange.wz.provider.properties.WzIntProperty;

public final class WzNodeUtil {
    public static void changeNodeName(WzObject wzObject, String oldName, String newName, int degree) {
        if (degree < 1) {
            return;
        } else if (degree == 1) {
            WzImageProperty prop = null;
            if (wzObject instanceof WzImage pImage) {
                pImage.parse();
                prop = pImage.getChild(oldName);
            } else if (wzObject instanceof WzImageProperty pProp && pProp.isListProperty()) {
                prop = pProp.getChild(oldName);
            }

            if (prop != null) {
                prop.setName(newName);
                prop.setTempChanged(true);
                prop.getWzImage().setChanged(true);
            }
        } else {
            degree--;
            if (wzObject instanceof WzImage image) {
                int finalDegree = degree;
                image.parse();
                image.getChildren().forEach(child -> changeNodeName(child, oldName, newName, finalDegree));
            } else if (wzObject instanceof WzImageProperty pProp && pProp.isListProperty()) {
                int finalDegree = degree;
                pProp.getChildren().forEach(child -> changeNodeName(child, oldName, newName, finalDegree));
            }
        }
    }

    public static void changeIntNodeValue(WzObject wzObject, String nodeName, int value) {
        if (wzObject instanceof WzIntProperty intProp && wzObject.getName().equals(nodeName)){
            intProp.setValue(value);
            intProp.setTempChanged(true);
            intProp.getWzImage().setChanged(true);
        }else if (wzObject instanceof WzImage image) {
            image.parse();
            image.getChildren().forEach(child -> changeIntNodeValue(child, nodeName, value));
        } else if (wzObject instanceof WzImageProperty pProp && pProp.isListProperty()) {
            pProp.getChildren().forEach(child -> changeIntNodeValue(child, nodeName, value));
        }
    }
}
