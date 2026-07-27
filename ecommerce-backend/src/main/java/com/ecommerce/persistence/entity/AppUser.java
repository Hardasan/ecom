package com.ecommerce.persistence.entity;

import com.ecommerce.persistence.entity.enumeration.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

/**
 * @author AmirHossein ZamanZade
 * @since 12/25/25
 */
@Entity
@Table(
        name = "app_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_app_user_mobile", columnNames = "mobile")
        }
)
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_seq")
    @SequenceGenerator(name = "app_user_seq", sequenceName = "app_user_seq", allocationSize = 50)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "password", nullable = false, length = 512)
    private String password;

    @Column(name = "is_registered", nullable = false)
    private Boolean isRegistered;

    @Column(name = "national_id", length = 20)
    private String nationalId;

    @Column(name = "iban", length = 26)
    private String iban;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "birth_date")
    private Date birthDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Date createdAt;

    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "IS_ENABLED", nullable = false, length = 1)
    private Boolean isEnabled;
}
