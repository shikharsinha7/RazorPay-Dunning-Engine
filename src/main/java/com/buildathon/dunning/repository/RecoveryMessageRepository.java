package com.buildathon.dunning.repository;

import com.buildathon.dunning.entity.RecoveryMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoveryMessageRepository extends JpaRepository<RecoveryMessage, Long> {

    List<RecoveryMessage> findByRetryAttempt_Payment_IdOrderBySentAtAsc(Long paymentId);
}
