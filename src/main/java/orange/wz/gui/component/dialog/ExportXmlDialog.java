package orange.wz.gui.component.dialog;

import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.data.ExportXmlData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.provider.tools.MediaExportType;
import orange.wz.provider.tools.XmlExportVersion;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public final class ExportXmlDialog extends BaseDialog<ExportXmlData> {
    private static final Preferences prefs = Preferences.userNodeForPackage(ExportXmlDialog.class);
    private final JTextField indentField = new JTextField(20);
    private final JRadioButton noneRadio = new JRadioButton("不输出");
    private final JRadioButton base64Radio = new JRadioButton("Base64");
    private final JRadioButton fileRadio = new JRadioButton("文件");
    private final JRadioButton windowsRadio = new JRadioButton("Windows CRLF \\r\\n");
    private final JRadioButton linuxRadio = new JRadioButton("Linux LF \\n");
    private final JRadioButton defaultVersionRadio = new JRadioButton("默认");
    private final JRadioButton v125VersionRadio = new JRadioButton("125");
    private final JTextField pathField = new JTextField(20);

    public ExportXmlDialog(EditPane editPane) {
        super("导出 XML", editPane);

        indentField.setText("2");
        JButton selectBtn = new JButton("选择");
        selectBtn.setSelected(false);
        selectBtn.addActionListener(e -> {
            File folder = FileDialog.chooseOpenFolder("选择导出目录");
            if (folder == null) return;

            pathField.setText(folder.getAbsolutePath());
        });
        pathField.setEditable(false);

        addRow("缩进数量", indentField);
        // 创建互斥单选集合
        ButtonGroup mediaGroup = new ButtonGroup();
        mediaGroup.add(noneRadio);
        mediaGroup.add(base64Radio);
        mediaGroup.add(fileRadio);
        noneRadio.setSelected(true);

        JPanel mediaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        mediaPanel.add(noneRadio);
        mediaPanel.add(base64Radio);
        mediaPanel.add(fileRadio);
        addRow("图片音频", mediaPanel);

        // 创建互斥单选集合
        ButtonGroup lineSepGroup = new ButtonGroup();
        lineSepGroup.add(windowsRadio);
        lineSepGroup.add(linuxRadio);

        String lineSeparator = prefs.get("lineSeparator", "windows");
        if (lineSeparator.equals("windows")) {
            windowsRadio.setSelected(true);
        } else {
            linuxRadio.setSelected(true);
        }

        JPanel lineSepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineSepPanel.add(windowsRadio);
        lineSepPanel.add(linuxRadio);
        addRow("换行符", lineSepPanel);

        ButtonGroup versionGroup = new ButtonGroup();
        versionGroup.add(defaultVersionRadio);
        versionGroup.add(v125VersionRadio);
        String version = prefs.get("xmlExportVersion", "default");
        if ("v125".equals(version) || "125".equals(version)) {
            v125VersionRadio.setSelected(true);
        } else {
            defaultVersionRadio.setSelected(true);
        }

        JPanel versionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        versionPanel.add(defaultVersionRadio);
        versionPanel.add(v125VersionRadio);
        addRow("导出版本", versionPanel);

        addRow("导出路径", pathField, selectBtn);
    }

    @Override
    public ExportXmlData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        if (pathField.getText().isBlank()) return null;

        int indent;
        try {
            indent = Integer.parseInt(indentField.getText());
        } catch (NumberFormatException e) {
            indent = 0;
        }

        MediaExportType meType = MediaExportType.NONE;
        if (base64Radio.isSelected()) {
            meType = MediaExportType.BASE64;
        } else if (fileRadio.isSelected()) {
            meType = MediaExportType.FILE;
        }

        boolean linux = false;
        if (windowsRadio.isSelected()) {
            prefs.put("lineSeparator", "windows");
        } else {
            prefs.put("lineSeparator", "linux");
            linux = true;
        }
        XmlExportVersion version;
        if (v125VersionRadio.isSelected()) {
            prefs.put("xmlExportVersion", "v125");
            version = XmlExportVersion.V125;
        } else {
            prefs.put("xmlExportVersion", "default");
            version = XmlExportVersion.DEFAULT;
        }
        return new ExportXmlData(indent, meType, pathField.getText(), linux, version);
    }

}
