package orange.wz.gui.form;

import orange.wz.gui.form.data.NodeFormData;

import javax.swing.*;

public interface FormPanel {
    JPanel getPanel();

    NodeFormData getData();
}

