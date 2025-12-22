package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.FloatFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class FloatDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public FloatDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public FloatFormData getData() {
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

        float value;
        try {
            value = Float.parseFloat(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new FloatFormData(
                nameField.getText(),
                "Float",
                value
        );
    }
}
