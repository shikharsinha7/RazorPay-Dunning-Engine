package com.buildathon.dunning.repository;

import com.buildathon.dunning.entity.FailureEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FailureEventRepository extends JpaRepository<FailureEvent, Long> {

    Optional<FailureEvent> findFirstByPayment_Id(Long paymentId);
}
