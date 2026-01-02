package orange.wz.gui.component.dialog;

import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.data.ExportXmlData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.provider.tools.MediaExportType;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public final class ExportXmlDialog extends BaseDialog<ExportXmlData> {
    private final JTextField indentField = new JTextField(20);
    private final JRadioButton base64Radio = new JRadioButton("Base64");
    private final JRadioButton fileRadio = new JRadioButton("文件");
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
        JRadioButton noneRadio = new JRadioButton("不输出");
        mediaGroup.add(noneRadio);
        mediaGroup.add(base64Radio);
        mediaGroup.add(fileRadio);
        noneRadio.setSelected(true);

        JPanel mediaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        mediaPanel.add(noneRadio);
        mediaPanel.add(base64Radio);
        mediaPanel.add(fileRadio);
        addRow("图片音频", mediaPanel);
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
        return new ExportXmlData(indent, meType, pathField.getText());
    }

}
