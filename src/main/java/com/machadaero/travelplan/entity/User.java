package com.machadaero.travelplan.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "Users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_provider_user"
        , columnNames = { "provider", "provider_user_id" })
}

)
@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    //각 컬럼은 null을 허용하고, 여러 컬럼을 묶은 조건으로 검증

    @Column()
    private String provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "user_local_id", unique = true)
    private  String userLocalId;

    @Column (name = "password_Hash")
    private  String passwordHash;

    
    @Column(name = "created_at" )
    private LocalDateTime createdAt;

    // JPA가 신규 엔터티를 영속화하기 전에 호출
    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    
}
