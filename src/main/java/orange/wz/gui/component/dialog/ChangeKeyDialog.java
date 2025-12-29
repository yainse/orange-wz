package orange.wz.gui.component.dialog;

import orange.wz.gui.MainFrame;
import orange.wz.gui.component.form.data.KeyData;
import orange.wz.gui.component.key.KeyBox;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.filter.IntegerFilter;
import orange.wz.gui.utils.JMessageUtil;
import orange.wz.provider.tools.wzkey.WzKey;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class ChangeKeyDialog extends JOptionPane {
    private final EditPane editPane;
    private final JPanel panel = new JPanel(new GridBagLayout());
    private int topPanelRow = 0;
    private final JTextField versionInput = new JTextField(20);
    private final KeyBox keyBox;

    public ChangeKeyDialog(EditPane editPane, boolean wzFile) {
        super();
        this.editPane = editPane;

        if (wzFile) {
            ((AbstractDocument) versionInput.getDocument()).setDocumentFilter(new IntegerFilter());
            addRow("版本号", versionInput);
        } else {
            versionInput.setText("-1");
        }

        WzKey[] wzKeys = MainFrame.getInstance().getWzKeyStorage().loadAll().toArray(new WzKey[0]);
        keyBox = new KeyBox(wzKeys);
        addRow("密钥", keyBox);
    }

    public KeyData getData() {
        int result = showConfirmDialog(
                editPane,
                panel,
                "修改密钥",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        short version;
        try {
            version = Short.parseShort(versionInput.getText());
        } catch (NumberFormatException e) {
            JMessageUtil.error("错误版本号 " + versionInput.getText());
            return null;
        }
        WzKey wzKey = (WzKey) keyBox.getSelectedItem();

        return new KeyData(version, wzKey.getIv(), wzKey.getUserKey());
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
