package orange.wz.gui.component.dialog;

import orange.wz.gui.component.FileDialog;
import orange.wz.gui.component.form.data.SoundFormData;
import orange.wz.gui.component.panel.EditPane;
import orange.wz.gui.utils.JMessageUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SoundDialog extends NodeDialog {
    private final JTextField pathField = new JTextField(20);
    private byte[] soundBytes;

    public SoundDialog(String title, EditPane editPane) {
        super(title, editPane);

        pathField.setEditable(false);
        JButton selectBtn = new JButton("选择音频");
        addRow("路径", pathField, selectBtn);

        selectBtn.addActionListener(e -> {
            File mp3File = FileDialog.chooseOpenFile(new String[]{"mp3"});
            if (mp3File == null) return;

            try {
                soundBytes = Files.readAllBytes(mp3File.toPath());
            } catch (IOException ex) {
                JMessageUtil.error("无法读取文件");
                return;
            }

            pathField.setText(mp3File.getAbsolutePath());
        });
    }

    public SoundFormData getData() {
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

        if (soundBytes == null) return null;
        return new SoundFormData(nameField.getText().trim(), "Sound", soundBytes);
    }
}
