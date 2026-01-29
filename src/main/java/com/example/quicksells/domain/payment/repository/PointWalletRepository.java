package com.example.quicksells.domain.payment.repository;

import com.example.quicksells.domain.payment.entity.PointWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointWalletRepository extends JpaRepository<PointWallet, Long> {
}
