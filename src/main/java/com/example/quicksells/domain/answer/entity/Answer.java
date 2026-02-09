package com.example.quicksells.domain.answer.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.domain.ask.entity.Ask;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ask_id")
    private Ask ask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted; // 삭제 여부

    public Answer(Ask ask, User admin, String title, String content) {

        this.ask = ask;
        this.admin = admin;
        this.title = title;
        this.content = content;
        this.isDeleted = false;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
