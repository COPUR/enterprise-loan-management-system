package com.bank.loanmanagement.loanorigination.infrastructure;

import com.bank.loanmanagement.loanorigination.domain.Loan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepository {

    private final JdbcTemplate jdbcTemplate;

    public LoanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Loan> LOAN_ROW_MAPPER = new RowMapper<Loan>() {
        @Override
        public Loan mapRow(ResultSet rs, int rowNum) throws SQLException {
            Loan loan = new Loan();
            loan.setId(rs.getLong("id"));
            loan.setLoanNumber(rs.getString("loan_number"));
            loan.setCustomerId(rs.getLong("customer_id"));
            loan.setPrincipalAmount(rs.getBigDecimal("principal_amount"));
            loan.setInstallmentCount(rs.getInt("installment_count"));
            loan.setMonthlyInterestRate(rs.getBigDecimal("monthly_interest_rate"));
            loan.setMonthlyPaymentAmount(rs.getBigDecimal("monthly_payment_amount"));
            loan.setTotalAmount(rs.getBigDecimal("total_amount"));
            loan.setOutstandingBalance(rs.getBigDecimal("outstanding_balance"));
            loan.setLoanStatus(rs.getString("loan_status"));
            loan.setDisbursementDate(rs.getTimestamp("disbursement_date") != null ? 
                rs.getTimestamp("disbursement_date").toLocalDateTime() : null);
            loan.setMaturityDate(rs.getDate("maturity_date") != null ? 
                rs.getDate("maturity_date").toLocalDate() : null);
            loan.setNextPaymentDate(rs.getDate("next_payment_date") != null ? 
                rs.getDate("next_payment_date").toLocalDate() : null);
            loan.setCreatedAt(rs.getTimestamp("created_at") != null ? 
                rs.getTimestamp("created_at").toLocalDateTime() : null);
            loan.setUpdatedAt(rs.getTimestamp("updated_at") != null ? 
                rs.getTimestamp("updated_at").toLocalDateTime() : null);
            loan.setVersion(rs.getInt("version"));
            return loan;
        }
    };

    public List<Loan> findAll() {
        String sql = "SELECT * FROM loan_origination.loans ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER);
    }

    public Optional<Loan> findById(Long id) {
        String sql = "SELECT * FROM loan_origination.loans WHERE id = ?";
        List<Loan> loans = jdbcTemplate.query(sql, LOAN_ROW_MAPPER, id);
        return loans.isEmpty() ? Optional.empty() : Optional.of(loans.get(0));
    }

    public List<Loan> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM loan_origination.loans WHERE customer_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER, customerId);
    }

    public Optional<Loan> findByLoanNumber(String loanNumber) {
        String sql = "SELECT * FROM loan_origination.loans WHERE loan_number = ?";
        List<Loan> loans = jdbcTemplate.query(sql, LOAN_ROW_MAPPER, loanNumber);
        return loans.isEmpty() ? Optional.empty() : Optional.of(loans.get(0));
    }

    public Loan save(Loan loan) {
        if (loan.getId() == null) {
            return insert(loan);
        } else {
            return update(loan);
        }
    }

    private Loan insert(Loan loan) {
        String sql = """
            INSERT INTO loan_origination.loans (
                loan_number, customer_id, principal_amount, installment_count,
                monthly_interest_rate, monthly_payment_amount, total_amount, outstanding_balance,
                loan_status, disbursement_date, maturity_date, next_payment_date
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id, created_at, updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            loan.setId(rs.getLong("id"));
            loan.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            loan.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            loan.setVersion(rs.getInt("version"));
            return loan;
        }, loan.getLoanNumber(), loan.getCustomerId(), loan.getPrincipalAmount(),
           loan.getInstallmentCount(), loan.getMonthlyInterestRate(), loan.getMonthlyPaymentAmount(),
           loan.getTotalAmount(), loan.getOutstandingBalance(), loan.getLoanStatus(),
           loan.getDisbursementDate(), loan.getMaturityDate(), loan.getNextPaymentDate());
    }

    private Loan update(Loan loan) {
        String sql = """
            UPDATE loan_origination.loans SET
                outstanding_balance = ?, loan_status = ?, next_payment_date = ?,
                updated_at = CURRENT_TIMESTAMP, version = version + 1
            WHERE id = ? AND version = ?
            RETURNING updated_at, version
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            loan.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            loan.setVersion(rs.getInt("version"));
            return loan;
        }, loan.getOutstandingBalance(), loan.getLoanStatus(), loan.getNextPaymentDate(),
           loan.getId(), loan.getVersion());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM loan_origination.loans WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM loan_origination.loans";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    public List<Loan> findActiveLoans() {
        String sql = "SELECT * FROM loan_origination.loans WHERE loan_status = 'ACTIVE' ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, LOAN_ROW_MAPPER);
    }
}