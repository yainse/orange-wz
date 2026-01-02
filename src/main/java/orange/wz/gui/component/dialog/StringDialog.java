package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.StringFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public final class StringDialog extends NodeDialog {
    private final JTextField valueField = new JTextField(20);

    public StringDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("值", valueField);
    }

    @Override
    public StringFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        return new StringFormData(
                nameField.getText(),
                "String",
                valueField.getText()
        );
    }
}
