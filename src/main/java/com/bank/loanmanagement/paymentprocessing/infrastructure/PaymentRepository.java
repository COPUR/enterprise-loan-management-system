package com.bank.loanmanagement.paymentprocessing.infrastructure;

import com.bank.loanmanagement.paymentprocessing.domain.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Payment> PAYMENT_ROW_MAPPER = new RowMapper<Payment>() {
        @Override
        public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
            Payment payment = new Payment();
            payment.setId(rs.getLong("id"));
            payment.setPaymentNumber(rs.getString("payment_number"));
            payment.setLoanId(rs.getLong("loan_id"));
            payment.setCustomerId(rs.getLong("customer_id"));
            payment.setPaymentType(rs.getString("payment_type"));
            payment.setScheduledAmount(rs.getBigDecimal("scheduled_amount"));
            payment.setActualAmount(rs.getBigDecimal("actual_amount"));
            payment.setPrincipalAmount(rs.getBigDecimal("principal_amount"));
            payment.setInterestAmount(rs.getBigDecimal("interest_amount"));
            payment.setPenaltyAmount(rs.getBigDecimal("penalty_amount"));
            payment.setScheduledDate(rs.getDate("scheduled_date") != null ? 
                rs.getDate("scheduled_date").toLocalDate() : null);
            payment.setActualPaymentDate(rs.getTimestamp("actual_payment_date") != null ? 
                rs.getTimestamp("actual_payment_date").toLocalDateTime() : null);
            payment.setPaymentStatus(rs.getString("payment_status"));
            payment.setPaymentMethod(rs.getString("payment_method"));
            payment.setTransactionReference(rs.getString("transaction_reference"));
            payment.setProcessorReference(rs.getString("processor_reference"));
            payment.setFailureReason(rs.getString("failure_reason"));
            payment.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            payment.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            payment.setVersion(rs.getInt("version"));
            return payment;
        }
    };

    public List<Payment> findAll() {
        String sql = "SELECT * FROM payment_processing.payments ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER);
    }

    public Optional<Payment> findById(Long id) {
        String sql = "SELECT * FROM payment_processing.payments WHERE id = ?";
        List<Payment> payments = jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, id);
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public List<Payment> findByLoanId(Long loanId) {
        String sql = "SELECT * FROM payment_processing.payments WHERE loan_id = ? ORDER BY scheduled_date ASC";
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, loanId);
    }

    public List<Payment> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM payment_processing.payments WHERE customer_id = ? ORDER BY scheduled_date DESC";
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, customerId);
    }

    public Optional<Payment> findByPaymentNumber(String paymentNumber) {
        String sql = "SELECT * FROM payment_processing.payments WHERE payment_number = ?";
        List<Payment> payments = jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER, paymentNumber);
        return payments.isEmpty() ? Optional.empty() : Optional.of(payments.get(0));
    }

    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            return insert(payment);
        } else {
            return update(payment);
        }
    }

    private Payment insert(Payment payment) {
        String sql = """
            INSERT INTO payment_processing.payments (
                payment_number, loan_id, customer_id, payment_type, scheduled_amount,
                actual_amount, principal_amount, interest_amount, penalty_amount,
                scheduled_date, actual_payment_date, payment_status, payment_method,
                transaction_reference, processor_reference, failure_reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            payment.setId(rs.getLong("id"));
            payment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            payment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            payment.setVersion(rs.getInt("version"));
            return payment;
        }, payment.getPaymentNumber(), payment.getLoanId(), payment.getCustomerId(),
           payment.getPaymentType(), payment.getScheduledAmount(), payment.getActualAmount(),
           payment.getPrincipalAmount(), payment.getInterestAmount(), payment.getPenaltyAmount(),
           payment.getScheduledDate(), payment.getActualPaymentDate(), payment.getPaymentStatus(),
           payment.getPaymentMethod(), payment.getTransactionReference(), payment.getProcessorReference(),
           payment.getFailureReason());
    }

    private Payment update(Payment payment) {
        String sql = """
            UPDATE payment_processing.payments SET
                actual_amount = ?, actual_payment_date = ?, payment_status = ?,
                payment_method = ?, transaction_reference = ?, processor_reference = ?,
                failure_reason = ?, penalty_amount = ?, updated_at = CURRENT_TIMESTAMP, version = version + 1
            WHERE id = ? AND version = ?
            RETURNING updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            payment.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            payment.setVersion(rs.getInt("version"));
            return payment;
        }, payment.getActualAmount(), payment.getActualPaymentDate(), payment.getPaymentStatus(),
           payment.getPaymentMethod(), payment.getTransactionReference(), payment.getProcessorReference(),
           payment.getFailureReason(), payment.getPenaltyAmount(), payment.getId(), payment.getVersion());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM payment_processing.payments WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM payment_processing.payments";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    public List<Payment> findPendingPayments() {
        String sql = "SELECT * FROM payment_processing.payments WHERE payment_status = 'PENDING' ORDER BY scheduled_date ASC";
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER);
    }

    public List<Payment> findOverduePayments() {
        String sql = """
            SELECT * FROM payment_processing.payments 
            WHERE payment_status = 'PENDING' AND scheduled_date < CURRENT_DATE 
            ORDER BY scheduled_date ASC
            """;
        return jdbcTemplate.query(sql, PAYMENT_ROW_MAPPER);
    }
}