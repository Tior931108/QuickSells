package com.example.quicksells.domain.chat.model.response;

import com.example.quicksells.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class AvailableUsersResponse {

    private final List<UserSummary> admins; // 관리자 목록 (항상 채팅 가능)

    private final List<DealUserSummary> dealUsers; // 거래 상대방 목록 (경매 낙찰 후 채팅 가능)

    @Getter
    @AllArgsConstructor
    public static class UserSummary {

        private final Long userId;
        private final String name;
        private final String role;
        private final Boolean hasExistingChat; // 채팅방 존재 여부 (includeExisting=true일 때만 의미)

        public static UserSummary from(User user, boolean hasExistingChat) {
            return new UserSummary(
                    user.getId(),
                    user.getName(),
                    user.getRole().name(),
                    hasExistingChat
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DealUserSummary {

        private final Long userId;
        private final String name;

        private final List<DealInfo> deals; // 거래 목록 (동일 사용자의 여러 거래)

        private final Boolean hasExistingChat; // 채팅방 존재 여부

        // 동일 사용자의 여러 거래를 그룹핑
        public static DealUserSummary from(User user, List<DealInfo> deals, boolean hasExistingChat) {
            return new DealUserSummary(
                    user.getId(),
                    user.getName(),
                    deals,
                    hasExistingChat
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class DealInfo {

        private final Long dealId;
        private final String itemName;

        private final String dealRole; // 구매자, 판매자 표시

        public static DealInfo of(Long dealId, String itemName, String dealRole) {
            return new DealInfo(dealId, itemName, dealRole);
        }
    }
}
