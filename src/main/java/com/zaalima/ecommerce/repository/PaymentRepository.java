package com.zaalima.ecommerce.repository;

import com.zaalima.ecommerce.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentRecord, String> {
    Optional<PaymentRecord> findByOrderId(String orderId);
}
