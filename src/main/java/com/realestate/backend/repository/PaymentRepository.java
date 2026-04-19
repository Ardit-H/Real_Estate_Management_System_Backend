package com.realestate.backend.repository;

import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.rental.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Pagesat sipas kontratës
    List<Payment> findByContract_IdOrderByDueDateAsc(Long contractId);

    // Pagesat sipas statusit
    Page<Payment> findByStatusOrderByDueDateAsc(PaymentStatus status, Pageable pageable);

    // Pagesat e vonuara (PENDING me due_date të kaluar)
    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = com.realestate.backend.entity.enums.PaymentStatus.PENDING
          AND p.dueDate < :today
        ORDER BY p.dueDate ASC
    """)
    List<Payment> findOverduePayments(@Param("today") LocalDate today);

    // Pagesat e kontratës sipas statusit
    List<Payment> findByContract_IdAndStatusOrderByDueDateAsc(
            Long contractId, PaymentStatus status);

    // Totali i të ardhurave të paguara
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = com.realestate.backend.entity.enums.PaymentStatus.PAID
    """)
    BigDecimal totalRevenue();

    // Totali sipas kontratës
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.contract.id = :contractId
          AND p.status = com.realestate.backend.entity.enums.PaymentStatus.PAID
    """)
    BigDecimal totalPaidByContract(@Param("contractId") Long contractId);

    // Pagesat e muajit aktual
    @Query("""
        SELECT p FROM Payment p
        WHERE p.dueDate BETWEEN :from AND :to
        ORDER BY p.dueDate ASC
    """)
    List<Payment> findByDueDateBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // Ndrysho statusin
    @Modifying
    @Query("""
        UPDATE Payment p
        SET p.status = :status,
            p.paidDate = :paidDate
        WHERE p.id = :id
    """)
    void markAsPaid(
            @Param("id") Long id,
            @Param("status") PaymentStatus status,
            @Param("paidDate") LocalDate paidDate
    );

    // Numëro pagesat e vonuara
    long countByStatus(PaymentStatus status);
}