package com.example.quicksells.domain.item.repository;

import com.example.quicksells.common.config.QuerydslConfig;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class ItemRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("상품 전체 목록 조회 성공")
    void findItemList_success () {

        // given
        User seller = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        em.persist(seller);

        for (int i = 1; i <= 5; i++) {
            Item item = new Item(seller, "상품" + i, 100L + i, "설명" + i, "imageUrl" + i );
            em.persist(item);
        }

        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<Item> response = itemRepository.findItemList(pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getContent().get(0).getName()).isEqualTo("상품5");
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void findItemDetail_success () {

        // given
        User seller = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        em.persist(seller);

        Item item = new Item(seller, "상품", 100L, "설명", "imageUrl"  );
        em.persist(item);

        // when
        Optional<Item> response = itemRepository.findItemDetail(item.getId());

        // then
        assertThat(response).isNotNull();
    }
}