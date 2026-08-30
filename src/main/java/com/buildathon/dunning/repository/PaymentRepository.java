package com.buildathon.dunning.repository;

import com.buildathon.dunning.entity.Payment;
import com.buildathon.dunning.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStatus(PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
