package com.unionclass.auth_service.adaptor.out.mysql.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "auth")
public class AuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true)
    private String logInId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    private AuthEntity(
            String userId,
            String logInId,
            String password,
            String email,
            String name,
            String phone,
            boolean deleted
    ) {
        this.userId = userId;
        this.logInId = logInId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.deleted = deleted;
    }
}
