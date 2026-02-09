package com.example.quicksells.domain.item.entity;

import com.example.quicksells.common.entity.BaseEntity;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false") //false(삭제인 경우 조회 X)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String name;

    @Column(name = "hope_price", nullable = false)
    private Long hopePrice;

    @Column(length = 500, nullable = false)
    private String description;

    @Column(nullable = false)
    private String image;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Item(User seller, String name, Long hopePrice, String description, String image) {
        this.seller = seller;
        this.name = name;
        this.hopePrice = hopePrice;
        this.description = description;
        this.image = image;
    }

    public void update(String name, Long hopePrice, String description, String imageUrl) {
        this.name = name;
        this.hopePrice = hopePrice;
        this.description = description;
        this.image = imageUrl;

    }

    public void softDelete() {
        this.isDeleted = true;
    }
}