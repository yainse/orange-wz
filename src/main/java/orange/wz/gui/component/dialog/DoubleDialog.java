package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.DoubleFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class DoubleDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public DoubleDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public DoubleFormData getData() {
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

        double value;
        try {
            value = Double.parseDouble(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new DoubleFormData(
                nameField.getText(),
                "Double",
                value
        );
    }
}
