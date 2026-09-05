package com.example.personalfinancemanager.service;

import com.example.personalfinancemanager.dto.report.MonthlyReportResponse;
import com.example.personalfinancemanager.dto.report.YearlyReportResponse;
import com.example.personalfinancemanager.entity.Transaction;
import com.example.personalfinancemanager.entity.TransactionType;
import com.example.personalfinancemanager.entity.User;
import com.example.personalfinancemanager.exception.ValidationException;
import com.example.personalfinancemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public ReportService(TransactionRepository transactionRepository, UserService userService) {
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ValidationException("Invalid month: " + month + ". Month must be between 1 and 12.");
        }
        if (year < 1900 || year > 2100) {
            throw new ValidationException("Invalid year: " + year);
        }

        User user = userService.getCurrentAuthenticatedUser();

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount();

            if (t.getType() == TransactionType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == TransactionType.EXPENSE) {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new MonthlyReportResponse(month, year, totalIncome, totalExpenses, netSavings);
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(int year) {
        if (year < 1900 || year > 2100) {
            throw new ValidationException("Invalid year: " + year);
        }

        User user = userService.getCurrentAuthenticatedUser();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();

        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount();

            if (t.getType() == TransactionType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == TransactionType.EXPENSE) {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new YearlyReportResponse(year, totalIncome, totalExpenses, netSavings);
    }
}
