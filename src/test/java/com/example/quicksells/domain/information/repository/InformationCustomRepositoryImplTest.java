package com.example.quicksells.domain.information.repository;

import com.example.quicksells.common.config.QuerydslConfig;
import com.example.quicksells.domain.information.entity.Information;
import com.example.quicksells.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class InformationCustomRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private InformationRepository informationRepository;

    @Test
    @DisplayName("공지사항 페이징 조회 성공")
    void findInformationPageSummary_success() {

        // given
        User admin = new User("test@test.com", "encodedPassword", "홍길동", "010-0000-1111", "서울시 관악구", "20010101");
        em.persist(admin);


        for (int i = 1; i <= 5; i++) {
            Information information = new Information(admin, "제목" + i, "내용" + i, "imageUrl" + i);
            em.persist(information);
        }

        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<Information> response = informationRepository.findInformationPageSummary(pageable);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("제목5");
    }
}
