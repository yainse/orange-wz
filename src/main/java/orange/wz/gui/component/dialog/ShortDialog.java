package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.ShortFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class ShortDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public ShortDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public ShortFormData getData() {
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

        short value;
        try {
            value = Short.parseShort(valueField.getText());
        } catch (NumberFormatException e) {
            value = 0;
        }

        return new ShortFormData(
                nameField.getText(),
                "Short",
                value
        );
    }
}
