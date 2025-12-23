package orange.wz.gui.component.dialog;

import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.data.ExportXmlData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public final class ExportXmlDialog extends JOptionPane {
    private final EditPane editPane;
    private int topPanelRow = 0;
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final JTextField indentField = new JTextField(20);
    private final JCheckBox mediaCheck = new JCheckBox("输出 Base64");
    private final JTextField pathField = new JTextField(20);

    public ExportXmlDialog(EditPane editPane, boolean wzFile) {
        super();
        this.editPane = editPane;

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
        addRow("图片音频", mediaCheck);
        addRow("导出路径", pathField, selectBtn);
    }

    public ExportXmlData getData() {
        int result = showConfirmDialog(
                editPane,
                panel,
                "导出 XML",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        if (pathField.getText().isBlank()) return null;

        int indent;
        try {
            indent = Integer.parseInt(indentField.getText());
        } catch (NumberFormatException e) {
            indent = 0;
        }

        return new ExportXmlData(indent, mediaCheck.isSelected(), pathField.getText());
    }

    private void addRow(String label, JComponent... fields) {
        // 标签 gbc
        GridBagConstraints labelGbc = baseGbc();
        labelGbc.gridx = 0;
        labelGbc.gridy = topPanelRow;
        labelGbc.weightx = 0; // 标签不拉伸
        panel.add(new JLabel(label), labelGbc);

        // 输入组件
        for (int i = 0; i < fields.length; i++) {
            GridBagConstraints fieldGbc = baseGbc();
            fieldGbc.gridx = i + 1;
            fieldGbc.gridy = topPanelRow;

            // 只有最后一个组件拉伸
            if (i == fields.length - 1) {
                fieldGbc.weightx = 1.0;
                fieldGbc.fill = GridBagConstraints.HORIZONTAL;
            } else {
                fieldGbc.weightx = 0;
            }

            panel.add(fields[i], fieldGbc);
        }

        topPanelRow++;
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }
}
