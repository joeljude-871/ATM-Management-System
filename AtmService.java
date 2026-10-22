package com.atm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AtmService {
    public record Account(long id, String cardNumber, String customerName, BigDecimal balance) {}
    public record Txn(String type, BigDecimal amount, BigDecimal balanceAfter, Timestamp createdAt) {}

    public boolean createAccount(String card, String name, String pin, BigDecimal openingBalance) throws SQLException {
        if (card == null || !card.matches("\\d{8,20}"))
            throw new IllegalArgumentException("Card number must contain 8–20 digits.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Customer name is required.");
        if (!PinUtil.isValidPin(pin))
            throw new IllegalArgumentException("PIN must contain 4–6 digits.");
        BigDecimal amount = money(openingBalance);
        if (amount.signum() < 0)
            throw new IllegalArgumentException("Opening balance cannot be negative.");

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO accounts(card_number, customer_name, pin_hash, balance) VALUES(?,?,?,?)")) {
            ps.setString(1, card);
            ps.setString(2, name.trim());
            ps.setString(3, PinUtil.hash(pin));
            ps.setBigDecimal(4, amount);
            return ps.executeUpdate() == 1;
        }
    }

    public Account login(String card, String pin) throws SQLException {
        String sql = "SELECT account_id, card_number, customer_name, pin_hash, balance FROM accounts WHERE card_number = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, card);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !PinUtil.verify(pin, rs.getString("pin_hash"))) return null;
                return new Account(rs.getLong("account_id"), rs.getString("card_number"),
                        rs.getString("customer_name"), rs.getBigDecimal("balance"));
            }
        }
    }

    public BigDecimal getBalance(long accountId) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT balance FROM accounts WHERE account_id = ?")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Account not found.");
                return rs.getBigDecimal(1);
            }
        }
    }

    public BigDecimal deposit(long accountId, BigDecimal amount) throws SQLException {
        BigDecimal value = validateAmount(amount);
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal current = lockBalance(con, accountId);
                BigDecimal updated = current.add(value);
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=? WHERE account_id=?")) {
                    ps.setBigDecimal(1, updated);
                    ps.setLong(2, accountId);
                    ps.executeUpdate();
                }
                recordTransaction(con, accountId, "DEPOSIT", value, updated);
                con.commit();
                return updated;
            } catch (Exception e) {
                con.rollback();
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException(e);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public BigDecimal withdraw(long accountId, BigDecimal amount) throws SQLException {
        BigDecimal value = validateAmount(amount);
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal current = lockBalance(con, accountId);
                if (current.compareTo(value) < 0)
                    throw new IllegalArgumentException("Insufficient balance.");
                BigDecimal updated = current.subtract(value);
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET balance=? WHERE account_id=?")) {
                    ps.setBigDecimal(1, updated);
                    ps.setLong(2, accountId);
                    ps.executeUpdate();
                }
                recordTransaction(con, accountId, "WITHDRAWAL", value, updated);
                con.commit();
                return updated;
            } catch (Exception e) {
                con.rollback();
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException(e);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Txn> getHistory(long accountId) throws SQLException {
        List<Txn> list = new ArrayList<>();
        String sql = "SELECT transaction_type, amount, balance_after, created_at FROM transactions WHERE account_id=? ORDER BY created_at DESC LIMIT 20";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Txn(rs.getString(1), rs.getBigDecimal(2),
                        rs.getBigDecimal(3), rs.getTimestamp(4)));
            }
        }
        return list;
    }

    public void changePin(long accountId, String oldPin, String newPin) throws SQLException {
        if (!PinUtil.isValidPin(newPin))
            throw new IllegalArgumentException("New PIN must contain 4–6 digits.");
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                String stored;
                try (PreparedStatement ps = con.prepareStatement("SELECT pin_hash FROM accounts WHERE account_id=? FOR UPDATE")) {
                    ps.setLong(1, accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Account not found.");
                        stored = rs.getString(1);
                    }
                }
                if (!PinUtil.verify(oldPin, stored))
                    throw new IllegalArgumentException("Current PIN is incorrect.");
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET pin_hash=? WHERE account_id=?")) {
                    ps.setString(1, PinUtil.hash(newPin));
                    ps.setLong(2, accountId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                if (e instanceof SQLException se) throw se;
                if (e instanceof RuntimeException re) throw re;
                throw new SQLException(e);
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private BigDecimal lockBalance(Connection con, long accountId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT balance FROM accounts WHERE account_id=? FOR UPDATE")) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Account not found.");
                return rs.getBigDecimal(1);
            }
        }
    }

    private void recordTransaction(Connection con, long accountId, String type, BigDecimal amount, BigDecimal balance) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO transactions(account_id, transaction_type, amount, balance_after) VALUES(?,?,?,?)")) {
            ps.setLong(1, accountId);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, balance);
            ps.executeUpdate();
        }
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        BigDecimal value = money(amount);
        if (value.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        return value;
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required.");
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount can have at most 2 decimal places.");
        }
    }
}
