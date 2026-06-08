package com.technext.crm.service;

import com.technext.crm.model.Leave;
import com.technext.crm.model.LeaveBalance;
import com.technext.crm.repository.LeaveRepository;
import com.technext.crm.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    public List<Leave> getAllLeaves() {
        return leaveRepository.findByOrderByCreatedAtDesc();
    }

    public List<Leave> getLeavesByEmployee(Integer employeeId) {
        return leaveRepository.findByEmployeeId(employeeId);
    }

    public List<Leave> getLeavesByStatus(String status) {
        return leaveRepository.findByStatus(status);
    }

    public Leave applyLeave(Leave leave) {
        // Auto-initialize leave balance if not exists
        autoInitLeaveBalance(leave.getEmployeeId(), leave.getEmployeeName());
        leave.setStatus("Pending");
        leave.setCreatedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    public Leave approveLeave(Integer id, String approvedBy) {
        Leave leave = leaveRepository.findById(id).orElseThrow();
        leave.setStatus("Approved");
        leave.setApprovedBy(approvedBy);
        leave.setApprovedAt(LocalDateTime.now());

        // Deduct from leave balance for paid leaves
        if (!leave.getLeaveType().equals("Loss of Pay")) {
            Optional<LeaveBalance> balanceOpt = leaveBalanceRepository
                .findByEmployeeId(leave.getEmployeeId());
            if (balanceOpt.isPresent()) {
                LeaveBalance balance = balanceOpt.get();
                switch (leave.getLeaveType()) {
                    case "Casual Leave":
                        balance.setCasualLeaveUsed(
                            balance.getCasualLeaveUsed() + leave.getDays());
                        break;
                    case "Sick Leave":
                        balance.setSickLeaveUsed(
                            balance.getSickLeaveUsed() + leave.getDays());
                        break;
                    case "Annual Leave":
                        balance.setAnnualLeaveUsed(
                            balance.getAnnualLeaveUsed() + leave.getDays());
                        break;
                }
                leaveBalanceRepository.save(balance);
            }
        }
        return leaveRepository.save(leave);
    }

    public Leave rejectLeave(Integer id, String rejectedBy) {
        Leave leave = leaveRepository.findById(id).orElseThrow();
        leave.setStatus("Rejected");
        leave.setApprovedBy(rejectedBy);
        leave.setApprovedAt(LocalDateTime.now());
        return leaveRepository.save(leave);
    }

    public Leave cancelLeave(Integer id) {
        Leave leave = leaveRepository.findById(id).orElseThrow();
        // Restore balance if previously approved
        if ("Approved".equals(leave.getStatus()) &&
            !leave.getLeaveType().equals("Loss of Pay")) {
            Optional<LeaveBalance> balanceOpt = leaveBalanceRepository
                .findByEmployeeId(leave.getEmployeeId());
            if (balanceOpt.isPresent()) {
                LeaveBalance balance = balanceOpt.get();
                switch (leave.getLeaveType()) {
                    case "Casual Leave":
                        balance.setCasualLeaveUsed(
                            Math.max(0, balance.getCasualLeaveUsed() - leave.getDays()));
                        break;
                    case "Sick Leave":
                        balance.setSickLeaveUsed(
                            Math.max(0, balance.getSickLeaveUsed() - leave.getDays()));
                        break;
                    case "Annual Leave":
                        balance.setAnnualLeaveUsed(
                            Math.max(0, balance.getAnnualLeaveUsed() - leave.getDays()));
                        break;
                }
                leaveBalanceRepository.save(balance);
            }
        }
        leave.setStatus("Cancelled");
        return leaveRepository.save(leave);
    }

    // Auto-initialize leave balance when not exists
    public LeaveBalance autoInitLeaveBalance(Integer employeeId, String employeeName) {
        Optional<LeaveBalance> existing = leaveBalanceRepository.findByEmployeeId(employeeId);
        if (existing.isPresent()) return existing.get();

        LeaveBalance balance = new LeaveBalance();
        balance.setEmployeeId(employeeId);
        balance.setEmployeeName(employeeName);
        balance.setCasualLeaveTotal(12);
        balance.setCasualLeaveUsed(0);
        balance.setSickLeaveTotal(12);
        balance.setSickLeaveUsed(0);
        balance.setAnnualLeaveTotal(15);
        balance.setAnnualLeaveUsed(0);
        balance.setYear(java.time.LocalDate.now().getYear());
        return leaveBalanceRepository.save(balance);
    }

    public LeaveBalance getLeaveBalance(Integer employeeId) {
        return leaveBalanceRepository.findByEmployeeId(employeeId).orElse(null);
    }

    public LeaveBalance initializeLeaveBalance(Integer employeeId, String employeeName) {
        return autoInitLeaveBalance(employeeId, employeeName);
    }

    public int getLopDaysForMonth(Integer employeeId, int month, int year) {
        List<Leave> leaves = leaveRepository.findByEmployeeId(employeeId);
        return (int) leaves.stream()
            .filter(l -> "Approved".equals(l.getStatus()))
            .filter(l -> "Loss of Pay".equals(l.getLeaveType()))
            .filter(l -> {
                int m = l.getFromDate().getMonthValue();
                int y = l.getFromDate().getYear();
                return m == month && y == year;
            })
            .mapToLong(Leave::getDays)
            .sum();
    }

    // Get total approved leave days (any type) for month — for payslip LOP
    public int getTotalAbsenceDaysForMonth(Integer employeeId, int month, int year) {
        // Count only LOP leaves — paid leaves don't cut salary
        return getLopDaysForMonth(employeeId, month, year);
    }
}