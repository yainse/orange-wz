package orange.wz.gui.component.dialog;

import lombok.Getter;
import orange.wz.gui.component.form.data.NodeFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;

@Getter
public class NodeDialog extends JOptionPane {
    protected final EditPane editPane;
    protected final JPanel panel = new JPanel(new GridBagLayout());
    protected final JTextField nameField = new JTextField(20);
    protected final String title;

    private int topPanelRow = 0;

    public NodeDialog(String title, EditPane editPane) {
        super();
        this.title = title;
        this.editPane = editPane;

        addRow("名称", nameField);

        // 显示的时候光标聚焦在Name输入框
        nameField.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && nameField.isShowing()) {
                SwingUtilities.invokeLater(nameField::requestFocusInWindow);
            }
        });
    }

    public NodeFormData getData() {
        int result = showConfirmDialog(
                editPane,
                panel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        return new NodeFormData(nameField.getText().trim(), "List");
    }


    protected void addRow(String label, JComponent... fields) {
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

    protected GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }
}
