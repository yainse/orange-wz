package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.StringFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class StringDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public StringDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    public StringFormData getData() {
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

        return new StringFormData(
                nameField.getText(),
                "String",
                valueField.getText()
        );
    }
}
