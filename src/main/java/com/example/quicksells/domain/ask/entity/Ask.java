package com.example.quicksells.domain.ask.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.common.enums.AskType;
import com.example.quicksells.domain.ask.model.request.AskUpdateRequest;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.Optional;

@Table(name = "asks")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Ask extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "ask_type", length = 20, nullable = false)
    private AskType askType; // 문의 유형

    @Column(length = 50, nullable = false)
    private String title;

    @Column(length = 500, nullable = false)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Ask(User user, AskType askType, String title, String content) {
        this.user = user;
        this.askType = askType;
        this.title = title;
        this.content = content;
        this.isDeleted = false;
    }

    // 문의 부분 수정
    public void updatePartial(AskUpdateRequest request) {

        // 문의 유형은 필수
        this.askType = request.getAskType();

        // 문의 제목과 내용은 부분 수정 가능
        Optional.ofNullable(request.getTitle())
                .ifPresent(v -> this.title = v);

        Optional.ofNullable(request.getContent())
                .ifPresent(v -> this.content = v);
    }

    // 문의 삭제 (Soft Delete)
    public void delete() {
        this.isDeleted = true;
    }

    // 본인 작성 문의인지 확인
    public boolean isWrittenBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
