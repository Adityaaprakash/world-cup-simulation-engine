package com.aditya.worldcup.managers.repository;

import com.aditya.worldcup.managers.entity.ResourceTransaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceTransactionRepository extends JpaRepository<ResourceTransaction, Long> {
    List<ResourceTransaction> findByManagerIdOrderByTransactionDateDesc(Long managerId);
    boolean existsByManagerIdAndIdempotencyKey(Long managerId, String idempotencyKey);
}
