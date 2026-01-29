package com.example.quicksells.domain.payment.repository;

import com.example.quicksells.domain.payment.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}
