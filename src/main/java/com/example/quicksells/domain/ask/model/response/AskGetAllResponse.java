package com.example.quicksells.domain.ask.model.response;

import com.example.quicksells.domain.ask.entity.Ask;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AskGetAllResponse {

    private final Long askId;
    private final String title;
    private final String maskedUserName;// 마스킹된 유저 이름
    private final String askType;
    private final LocalDateTime createdAt;

    // Entity -> DTO 변환 (유저 이름 마스킹)
    public static AskGetAllResponse from(Ask ask) {
        return new AskGetAllResponse(
                ask.getId(),
                ask.getTitle(),
                maskUserName(ask.getUser().getName()), // 마스킹 처리
                ask.getAskType().name(),
                ask.getCreatedAt()
        );
    }

    /**
     * 유저 이름 마스킹 처리
     * - 2글자: "홍길" → "홍**"
     * - 3글자 이상: "홍길동" → "홍**", "홍길길덕" → "홍**"
     * - 이름이 2글자인 외자인 경우, * 한개만 표시할 경우 누구인지 특정이 된다.
     * - 이름이 4글자 이상인경우에 마스킹 갯수를 그대로 보이게 한다면, 이 또한 누구인지 특정이 되어 마스킹의 의미가 없다.
     * > 성만 보이게 하고 이름은 글자수에 관계없이 ** 마스킹 처리
     */
    private static String maskUserName(String name) {

        // 이름 마스킹 : 첫 글자(성) + **
        String firstChar = name.substring(0, 1);
        String lastChar = name.substring(name.length() - 1);

        return firstChar + "**";
    }

}
