package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.VectorFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;

public class VectorDialog extends NodeDialog {
    private final JTextField xField = new JTextField(20);
    private final JTextField yField = new JTextField(20);

    public VectorDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("X", xField);
        addRow("Y", yField);
    }

    public VectorFormData getData() {
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

        int x, y;
        try {
            x = Integer.parseInt(xField.getText());
            y = Integer.parseInt(yField.getText());
        } catch (NumberFormatException e) {
            x = 0;
            y = 0;
        }

        return new VectorFormData(
                nameField.getText(),
                "UOL",
                x,
                y
        );
    }
}
