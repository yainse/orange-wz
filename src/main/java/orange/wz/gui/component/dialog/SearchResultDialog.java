package orange.wz.gui.component.dialog;

import orange.wz.gui.component.form.data.SearchResult;
import orange.wz.gui.component.panel.EditPane;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;

public class SearchResultDialog extends JDialog {

    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private String filterText = "";

    public SearchResultDialog(Frame owner, String title, List<SearchResult> items, EditPane editPane) {
        super(owner, title, true);
        setModal(false);
        setSize(600, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // 搜索框
        JTextField searchField = new JTextField();
        searchField.setToolTipText("过滤");
        add(searchField, BorderLayout.NORTH);

        String[] columns = {"节点", "String值", "路径"};
        Object[][] data = new Object[items.size()][3];
        for (int i = 0; i < items.size(); i++) {
            SearchResult r = items.get(i);
            data[i][0] = r.name();
            data[i][1] = r.value();
            data[i][2] = r.getPathString();
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Name
        table.getColumnModel().getColumn(0).setMaxWidth(150); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(300); // Value
        table.getColumnModel().getColumn(1).setMaxWidth(300); // Value

        // 高亮渲染器
        TableCellRenderer highlightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String text = value != null ? value.toString() : "";
                setToolTipText(text);

                // 默认背景和文字颜色
                Color bg = isSelected ? table.getSelectionBackground() : table.getBackground();
                Color fg = isSelected ? table.getSelectionForeground() : table.getForeground();
                setBackground(bg);
                setForeground(fg);

                if (!filterText.isEmpty()) {
                    String regex = "(?i)" + Pattern.quote(filterText);
                    String highlighted = text.replaceAll(regex, "<span style='background:yellow;color:" +
                            (isSelected ? "black" : "red") + "'>$0</span>");
                    setText("<html>" + highlighted + "</html>");
                } else {
                    setText(text);
                }

                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(highlightRenderer);
        }

        // 双击事件
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        SearchResult r = items.get(modelRow);
                        editPane.selectTreeNodeByPath(r.path());
                    }
                }
            }
        });

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 搜索框实时过滤和高亮
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateFilter() {
                filterText = searchField.getText().trim();
                if (filterText.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filterText)));
                }
                table.repaint(); // 刷新渲染器，显示高亮
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateFilter();
            }
        });

        // 设置 Ctrl+F 快捷键
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow(); // 聚焦搜索框
                searchField.selectAll();            // 可选：选中已有内容
            }
        });
    }
}
