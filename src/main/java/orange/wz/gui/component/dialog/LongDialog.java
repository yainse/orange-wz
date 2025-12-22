package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.LongFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class LongDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public LongDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public LongFormData getData() {
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

        long value;
        try {
            value = Long.parseLong(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new LongFormData(
                nameField.getText(),
                "Long",
                value
        );
    }
}
