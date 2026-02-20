package com.example.quicksells.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatProfanityFilterServiceTest {

    private ChatProfanityFilterService filterService;

    @BeforeEach
    void setUp() {
        // 실제 서비스 객체 생성 (파일 로드 포함)
        filterService = new ChatProfanityFilterService();
    }

    @Test
    @DisplayName("비속어가 포함된 메시지를 마스킹 처리한다")
    void filterProfanity_WithProfanity_ShouldMask() {
        // Given
        String message = "이 ㅂㅅ아";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).contains("**");
        assertThat(filtered).doesNotContainIgnoringCase("ㅂㅅ");
    }

    @Test
    @DisplayName("대소문자 구분 없이 비속어를 필터링한다")
    void filterProfanity_CaseInsensitive_ShouldMask() {
        // Given
        String upperCase = "FUCK person";
        String lowerCase = "fuck person";
        String mixedCase = "FUck person";

        // When
        String filteredUpper = filterService.filterProfanity(upperCase);
        String filteredLower = filterService.filterProfanity(lowerCase);
        String filteredMixed = filterService.filterProfanity(mixedCase);

        // Then
        assertThat(filteredUpper).doesNotContainIgnoringCase("fuck");
        assertThat(filteredLower).doesNotContainIgnoringCase("fuck");
        assertThat(filteredMixed).doesNotContainIgnoringCase("fuck");
        assertThat(filteredUpper).contains("*");
    }

    @Test
    @DisplayName("여러 비속어가 포함된 메시지를 모두 필터링한다")
    void filterProfanity_MultipleProfanities_ShouldMaskAll() {
        // Given
        String message = "fuck ㅅㅂ fuck";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).doesNotContainIgnoringCase("fuck");
        assertThat(filtered).doesNotContainIgnoringCase("ㅅㅂ");
        assertThat(filtered).contains("*");
    }

    @Test
    @DisplayName("문장 중간에 있는 비속어를 필터링한다")
    void filterProfanity_ProfanityInMiddle_ShouldMask() {
        // Given
        String message = "필터링 ㅅㅂ 잘 되나요?";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).startsWith("필터링");
        assertThat(filtered).endsWith("잘 되나요?");
        assertThat(filtered).contains("**");
        assertThat(filtered).doesNotContainIgnoringCase("ㅅㅂ");
    }

    @Test
    @DisplayName("자간 띄우기로 우회한 비속어를 필터링한다")
    void filterProfanity_WithSpaceBetweenCharacters_ShouldMask() {
        // Given
        String message = "ㅅ ㅂ";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).contains("***");
    }

    @Test
    @DisplayName("여러 공백으로 띄운 비속어를 필터링한다")
    void filterProfanity_WithMultipleSpaces_ShouldMask() {
        // Given
        String message = "fu  ck";  // 공백 2개

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).contains("***");
    }

    @Test
    @DisplayName("null 입력시 null을 반환한다")
    void filterProfanity_WithNull_ShouldReturnNull() {
        // Given
        String message = null;

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isNull();
    }

    @Test
    @DisplayName("빈 문자열 입력시 빈 문자열을 반환한다")
    void filterProfanity_WithEmptyString_ShouldReturnEmpty() {
        // Given
        String message = "";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEmpty();
    }

    @Test
    @DisplayName("공백만 있는 문자열은 그대로 반환한다")
    void filterProfanity_WithWhitespaceOnly_ShouldReturnAsIs() {
        // Given
        String message = "   ";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("탭과 줄바꿈만 있는 문자열은 그대로 반환한다")
    void filterProfanity_WithTabAndNewline_ShouldReturnAsIs() {
        // Given
        String message = "\t\n  \n";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("비속어가 없는 정상 메시지는 변경되지 않는다")
    void filterProfanity_WithoutProfanity_ShouldNotChange() {
        // Given
        String message = "안녕하세요 좋은 하루 되세요";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("비속어의 부분 문자열은 필터링하지 않는다")
    void filterProfanity_PartialMatch_ShouldNotMask() {
        // Given
        String message = "발표 준비 중입니다";  // "발"이 비속어 목록에 있어도

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("특수문자만 있는 메시지는 변경되지 않는다")
    void filterProfanity_OnlySpecialCharacters_ShouldNotChange() {
        // Given
        String message = "!@#$%^&*()";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("숫자만 있는 메시지는 변경되지 않는다")
    void filterProfanity_OnlyNumbers_ShouldNotChange() {
        // Given
        String message = "1234567890";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo(message);
    }

    @Test
    @DisplayName("비속어 길이만큼 별표로 마스킹한다")
    void filterProfanity_MaskLength_ShouldMatchProfanityLength() {
        // Given
        String message = "시발";  // 2글자

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).isEqualTo("**");  // 2개의 별표
    }

    @Test
    @DisplayName("여러 비속어의 길이에 맞게 각각 마스킹한다")
    void filterProfanity_MultipleDifferentLengths_ShouldMaskCorrectly() {
        // Given
        String message = "시발 fuck";  // 2글자, 4글자

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        // 각각의 길이만큼 마스킹되어야 함
        assertThat(filtered).contains("**");
        assertThat(filtered).contains("****");
    }

    @ParameterizedTest
    @ValueSource(strings = {"시발", "개새끼", "fuck", "pussy"})
    @DisplayName("다양한 비속어를 필터링한다")
    void filterProfanity_VariousProfanities_ShouldMask(String profanity) {
        // Given
        String message = "이 사람은 " + profanity + " 입니다";

        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        assertThat(filtered).doesNotContainIgnoringCase(profanity);
        assertThat(filtered).contains("*");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("빈 값이나 공백은 변경하지 않는다")
    void filterProfanity_EmptyOrWhitespace_ShouldReturnAsIs(String message) {
        // When
        String filtered = filterService.filterProfanity(message);

        // Then
        if (message == null) {
            assertThat(filtered).isNull();
        } else {
            assertThat(filtered).isEqualTo(message);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "안녕하세요, 안녕하세요",
            "좋은 하루, 좋은 하루",
            "감사합니다, 감사합니다",
            "12345, 12345"
    })
    @DisplayName("정상 메시지는 변경되지 않는다")
    void filterProfanity_NormalMessages_ShouldNotChange(String input, String expected) {
        // When
        String filtered = filterService.filterProfanity(input);

        // Then
        assertThat(filtered).isEqualTo(expected);
    }
}