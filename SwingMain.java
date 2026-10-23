package com.atm;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SwingMain extends JFrame {
    private static final Color NAVY = new Color(22, 43, 71);
    private static final Color GREEN = new Color(19, 125, 90);
    private static final Color LIGHT = new Color(245, 248, 250);
    private final AtmService service = new AtmService();

    private AtmService.Account currentAccount;
    private JPanel root;
    private JTextField cardField;
    private JPasswordField pinField;

    public SwingMain() {
        super("ATM Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        showLogin();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new SwingMain().setVisible(true);
        });
    }

    private void setPage(JPanel panel) {
        root = panel;
        setContentPane(root);
        revalidate();
        repaint();
    }

    private JPanel page(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(28, 34, 28, 34));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(NAVY);
        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(new Color(90, 103, 115));
        sub.setBorder(new EmptyBorder(6, 0, 0, 0));
        header.add(heading);
        header.add(sub);
        panel.add(header, BorderLayout.NORTH);
        return panel;
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                new EmptyBorder(11, 17, 11, 17)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(text);

        // A subtle darker shade when the mouse is over the button.
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(color.darker());
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(color);
            }
        });
        return b;
    }

    private JTextField textField() {
        JTextField f = new JTextField(22);
        f.setFont(new Font("SansSerif", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 214, 222)),
                new EmptyBorder(9, 10, 9, 10)));
        return f;
    }

    private JPasswordField passwordField() {
        JPasswordField f = new JPasswordField(22);
        f.setFont(new Font("SansSerif", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 214, 222)),
                new EmptyBorder(9, 10, 9, 10)));
        return f;
    }

    private void showLogin() {
        JPanel panel = page("ATM MANAGEMENT SYSTEM", "Secure account access");
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(28, 34, 28, 34));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        JLabel loginTitle = new JLabel("Customer Login");
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 21));
        loginTitle.setForeground(NAVY);
        card.add(loginTitle, c);

        c.gridwidth = 1; c.gridy++;
        card.add(new JLabel("Card number"), c);
        c.gridx = 1;
        card.add(cardField = textField(), c);

        c.gridx = 0; c.gridy++;
        card.add(new JLabel("ATM PIN"), c);
        c.gridx = 1;
        card.add(pinField = passwordField(), c);

        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        JButton login = button("Login", GREEN);
        login.addActionListener(e -> handleLogin());
        card.add(login, c);

        c.gridy++;
        JButton create = button("Create Demo Account", NAVY);
        create.addActionListener(e -> showCreateAccount());
        card.add(create, c);

        JLabel note = new JLabel("Educational demo only — do not use real banking credentials.");
        note.setForeground(new Color(100, 110, 120));
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        c.gridy++;
        card.add(note, c);

        center.add(card);
        panel.add(center, BorderLayout.CENTER);
        setPage(panel);
        getRootPane().setDefaultButton(login);
    }

    private void handleLogin() {
        String card = cardField.getText().trim();
        String pin = new String(pinField.getPassword());
        if (card.isBlank() || pin.isBlank()) {
            message("Enter both card number and PIN.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            AtmService.Account account = service.login(card, pin);
            if (account == null) {
                message("Invalid card number or PIN.", JOptionPane.ERROR_MESSAGE);
                pinField.setText("");
                return;
            }
            currentAccount = account;
            showDashboard();
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void showCreateAccount() {
        JPanel panel = page("CREATE DEMO ACCOUNT", "Create a test account for this educational application");
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 28, 22, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Customer name"), c);
        JTextField name = textField();
        c.gridx = 1; form.add(name, c);

        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Card number (8–20 digits)"), c);
        JTextField card = textField();
        c.gridx = 1; form.add(card, c);

        c.gridx = 0; c.gridy++;
        form.add(new JLabel("PIN (4–6 digits)"), c);
        JPasswordField pin = passwordField();
        c.gridx = 1; form.add(pin, c);

        c.gridx = 0; c.gridy++;
        form.add(new JLabel("Opening balance"), c);
        JTextField balance = textField();
        balance.setText("0.00");
        c.gridx = 1; form.add(balance, c);

        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        JButton create = button("Create Account", new Color(23,54,93));
        form.add(create, c);
        create.addActionListener(e -> {
            try {
                BigDecimal amount = new BigDecimal(balance.getText().trim());
                service.createAccount(card.getText().trim(), name.getText().trim(),
                        new String(pin.getPassword()), amount);
                message("Account created successfully. You can now log in.", JOptionPane.INFORMATION_MESSAGE);
                showLogin();
            } catch (NumberFormatException ex) {
                message("Enter a valid opening balance, for example 500.00.", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException ex) {
                message(ex.getMessage(), JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) {
                    message("That card number already exists. Choose a different number.", JOptionPane.WARNING_MESSAGE);
                } else {
                    databaseError(ex);
                }
            }
        });

        c.gridy++;
        JButton back = button("Back to Login", NAVY);
        back.addActionListener(e -> showLogin());
        form.add(back, c);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(form);
        panel.add(holder, BorderLayout.CENTER);
        setPage(panel);
    }

    private void showDashboard() {
        JPanel panel = page("Welcome, " + currentAccount.customerName(),
                "Card ending in " + maskCard(currentAccount.cardNumber()) + "  •  Choose an operation");
        JPanel grid = new JPanel(new GridLayout(3, 3, 16, 16));
        grid.setOpaque(false);
        JButton balance = button("Check Balance", NAVY);
        JButton deposit = button("Deposit Money", new Color(24,121,78));
        JButton withdraw = button("Withdraw Money", new Color(183,121,31));
        JButton history = button("Mini Statement", NAVY);
        JButton changePin = button("Change PIN", new Color(15,118,110));
        JButton cardStatus = button("Card Status", new Color(80, 90, 110));
        JButton blockCard = button("Block My Card", new Color(180, 35, 24));
        JButton cardHistory = button("Card Activity History", new Color(90, 70, 130));
        JButton unblockCard = button("Unblock My Card", new Color(19, 125, 90));
        JButton logout = button("Logout", new Color(90, 100, 110));

        balance.addActionListener(e -> showBalance());
        deposit.addActionListener(e -> handleMoneyOperation(true));
        withdraw.addActionListener(e -> handleMoneyOperation(false));
        history.addActionListener(e -> showHistory());
        changePin.addActionListener(e -> showChangePin());
        cardStatus.addActionListener(e -> showCardStatus());
        blockCard.addActionListener(e -> handleBlockCard());
        cardHistory.addActionListener(e -> showCardHistory());
        unblockCard.addActionListener(e -> handleUnblockCard());
        logout.addActionListener(e -> {
            currentAccount = null;
            showLogin();
        });

        grid.add(balance);
        grid.add(deposit);
        grid.add(withdraw);
        grid.add(history);
        grid.add(changePin);
        grid.add(cardStatus);
        grid.add(blockCard);
        grid.add(cardHistory);
        grid.add(unblockCard);
        grid.add(logout);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(grid);
        panel.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("ATM Management System • Java Swing + MySQL");
        footer.setForeground(new Color(100, 110, 120));
        panel.add(footer, BorderLayout.SOUTH);
        setPage(panel);
    }

    private void showCardStatus() {
        try {
            String status = service.getCardStatus(currentAccount.id());
            message("Current card status: " + status, status.equals("ACTIVE")
                    ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void handleBlockCard() {
        try {
            if ("BLOCKED".equals(service.getCardStatus(currentAccount.id()))) {
                message("This card is already blocked.", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JPanel form = new JPanel(new GridLayout(2, 2, 10, 10));
            JPasswordField pin = passwordField();
            JTextField reason = textField();
            form.add(new JLabel("Verify ATM PIN:"));
            form.add(pin);
            form.add(new JLabel("Reason for blocking:"));
            form.add(reason);
            int result = JOptionPane.showConfirmDialog(this, form, "Block ATM Card",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            service.blockCard(currentAccount.id(), new String(pin.getPassword()), reason.getText());
            message("Your card is now BLOCKED. Card transactions are disabled.", JOptionPane.INFORMATION_MESSAGE);
            showDashboard();
        } catch (IllegalArgumentException ex) {
            message(ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void handleUnblockCard() {
        try {
            if ("ACTIVE".equals(service.getCardStatus(currentAccount.id()))) {
                message("Your card is already active.", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            JPasswordField pin = passwordField();
            int result = JOptionPane.showConfirmDialog(this, new Object[] {
                    new JLabel("Verify your ATM PIN to request unblocking:"), pin
            }, "Verify and Unblock Card", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "After PIN verification, your card will be returned to ACTIVE status. Continue?",
                    "Confirm Unblocking", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            service.unblockCard(currentAccount.id(), new String(pin.getPassword()));
            message("PIN verified. Your card is now ACTIVE.", JOptionPane.INFORMATION_MESSAGE);
            showDashboard();
        } catch (IllegalArgumentException ex) {
            message(ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void showCardHistory() {
        try {
            List<AtmService.CardAction> actions = service.getCardHistory(currentAccount.id());
            String[] columns = {"Date and time", "Action", "Reason"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (AtmService.CardAction action : actions) {
                model.addRow(new Object[] {
                        action.createdAt().toLocalDateTime().format(formatter),
                        action.action(), action.reason()
                });
            }
            JTable table = new JTable(model);
            table.setRowHeight(26);
            table.setFillsViewportHeight(true);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(680, 300));
            JOptionPane.showMessageDialog(this, scroll, "Card Blocking/Unblocking History",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void showBalance() {
        try {
            BigDecimal balance = service.getBalance(currentAccount.id());
            JOptionPane.showMessageDialog(this, "Available balance: " + balance.toPlainString(),
                    "Account Balance", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void handleMoneyOperation(boolean isDeposit) {
        String label = isDeposit ? "Enter deposit amount:" : "Enter withdrawal amount:";
        String value = JOptionPane.showInputDialog(this, label, isDeposit ? "Deposit" : "Withdrawal",
                JOptionPane.QUESTION_MESSAGE);
        if (value == null) return;
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            BigDecimal updated = isDeposit
                    ? service.deposit(currentAccount.id(), amount)
                    : service.withdraw(currentAccount.id(), amount);
            message((isDeposit ? "Deposit" : "Withdrawal") + " successful.\nNew balance: " + updated.toPlainString(),
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            message("Enter a valid amount, for example 250.00.", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            message(ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void showHistory() {
        try {
            List<AtmService.Txn> txns = service.getHistory(currentAccount.id());
            String[] columns = {"Date and time", "Type", "Amount", "Balance after"};
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (AtmService.Txn txn : txns) {
                model.addRow(new Object[] {
                        txn.createdAt().toLocalDateTime().format(formatter),
                        txn.type(), txn.amount().toPlainString(), txn.balanceAfter().toPlainString()
                });
            }
            JTable table = new JTable(model);
            table.setRowHeight(26);
            table.setFillsViewportHeight(true);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(680, 300));
            JOptionPane.showMessageDialog(this, scroll, "Mini Statement — Last 20 Transactions",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private void showChangePin() {
        JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
        JPasswordField oldPin = passwordField();
        JPasswordField newPin = passwordField();
        JPasswordField confirmPin = passwordField();
        form.add(new JLabel("Current PIN:")); form.add(oldPin);
        form.add(new JLabel("New PIN (4–6 digits):")); form.add(newPin);
        form.add(new JLabel("Confirm new PIN:")); form.add(confirmPin);

        int result = JOptionPane.showConfirmDialog(this, form, "Change ATM PIN",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String oldValue = new String(oldPin.getPassword());
        String newValue = new String(newPin.getPassword());
        String confirmValue = new String(confirmPin.getPassword());
        if (!newValue.equals(confirmValue)) {
            message("The new PIN and confirmation do not match.", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            service.changePin(currentAccount.id(), oldValue, newValue);
            message("PIN changed successfully.", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            message(ex.getMessage(), JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            databaseError(ex);
        }
    }

    private String maskCard(String card) {
        return card.length() <= 4 ? card : "•••• " + card.substring(card.length() - 4);
    }

    private void message(String text, int type) {
        JOptionPane.showMessageDialog(this, text, "ATM Management System", type);
    }

    private void databaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Could not connect to or use the database.\n\nDetails: " + ex.getMessage()
                        + "\n\nCheck that MySQL Server is running and Database.java has the correct password.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
