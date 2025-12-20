package orange.wz.gui.utils;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static java.awt.Desktop.*;

@Slf4j
public final class UrlUtil {
    public static void open(String url) {
        if (!isDesktopSupported()) {
            JMessageUtil.warn("当前系统不支持打开浏览器");
            return;
        }

        Desktop desktop = getDesktop();
        if (!desktop.isSupported(Action.BROWSE)) {
            JMessageUtil.warn("当前系统不支持浏览操作");
            return;
        }

        try {
            desktop.browse(URI.create(url));
        } catch (IOException ex) {
            JMessageUtil.error("打开网址失败");
        }
    }
}
