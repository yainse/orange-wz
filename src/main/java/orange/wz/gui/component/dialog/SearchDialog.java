package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.SearchFormData;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class SearchDialog extends BaseDialog<SearchFormData> {
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox checkNameMod = new JCheckBox("名称");
    private final JCheckBox checkValueMod = new JCheckBox("String值");
    private final JCheckBox checkEqualMod = new JCheckBox("完整匹配");
    private final JCheckBox checkLowMod = new JCheckBox("忽略大小写");
    private final JRadioButton checkGlobalMod = new JRadioButton("搜全局");
    private final JCheckBox checkParseImgMod = new JCheckBox("自动解析IMG");

    public SearchDialog(String title, EditPane editPane) {
        super(title, editPane);

        addRow("目标", searchField);

        JPanel options1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options1.add(checkNameMod);
        checkNameMod.setSelected(true);
        options1.add(checkValueMod);
        options1.add(checkEqualMod);
        options1.add(checkLowMod);
        addRow("搜索", options1);

        ButtonGroup mediaGroup = new ButtonGroup();
        JRadioButton checkSelectedMod = new JRadioButton("搜选中");
        mediaGroup.add(checkSelectedMod);
        checkSelectedMod.setSelected(true);
        mediaGroup.add(checkGlobalMod);
        JPanel options2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options2.add(checkSelectedMod);
        options2.add(checkGlobalMod);
        addRow("范围", options2);

        JPanel options3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        options3.add(checkParseImgMod);
        checkParseImgMod.setSelected(true);
        addRow("选项", options3);
    }

    @Override
    protected int showDialog() {
        // 创建 JOptionPane
        JOptionPane optionPane = new JOptionPane(
                panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );

        // 创建 JDialog
        JDialog dialog = optionPane.createDialog(editPane, title);

        // 注册窗口打开事件，打开时聚焦 searchField
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (searchField.isVisible() && searchField.isEnabled()) {
                    searchField.requestFocusInWindow();
                    searchField.selectAll();
                }
            }
        });

        // 显示对话框（阻塞）
        dialog.setVisible(true);

        // 获取用户选择结果
        Object value = optionPane.getValue();
        return (value instanceof Integer) ? (Integer) value : JOptionPane.CLOSED_OPTION;
    }

    @Override
    public SearchFormData getData() {
        if (showDialog() != JOptionPane.OK_OPTION) {
            return null;
        }

        if (searchField.getText().isBlank()) return null;

        return new SearchFormData(
                searchField.getText(),
                checkNameMod.isSelected(),
                checkValueMod.isSelected(),
                checkEqualMod.isSelected(),
                checkLowMod.isSelected(),
                checkParseImgMod.isSelected(),
                checkGlobalMod.isSelected()
        );
    }
}
