package com.aditya.worldcup.managers.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "idempotency_key")
    private String idempotencyKey;
}
