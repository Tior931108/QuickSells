package com.example.quicksells.domain.user.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Table(name = "users")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50 ,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(length = 25)
    private String phone;

    private String address;

    @Column(length = 25)
    private String birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRole role;

    @Column(nullable = false)
    private boolean isDeleted;

    // 회원가입
    public User(String email, String password, String name, String phone, String address, String birth){
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.birth = birth;
        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
        this.isDeleted = false;
    }

    // 소셜 로그인
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = UserRole.USER;
        this.status = UserStatus.PENDING;
        this.isDeleted = false;
        this.phone = null;
        this.address = null;
        this.birth = null;
    }

    // 소셜 로그인 이후 추가 정보 입력
    public void completeSignup(String phone, String address, String birth){
        this.phone = phone;
        this.address = address;
        this.birth = birth;
        this.status = UserStatus.ACTIVE;
    }

    public void updatePhone(String phone) {this.phone = phone;}

    public void updateAddress(String address) {this.address = address;}

    public void updatePassword(String encodedPassword) {this.password = encodedPassword;}

    public void updateRole(String role) {this.role = UserRole.of(role);}

    public void delete() {this.isDeleted = true;}
}
