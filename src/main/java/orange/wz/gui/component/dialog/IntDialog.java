package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.IntFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class IntDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public IntDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public IntFormData getData() {
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

        int value;
        try {
            value = Integer.parseInt(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new IntFormData(
                nameField.getText(),
                "Int",
                value
        );
    }
}
