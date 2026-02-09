package com.example.quicksells.domain.appraise.model.response;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.domain.appraise.entity.Appraise;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AppraiseAdminGetResponse {

    // 판매자 정보
    private SellerDto seller;

    // 상품 상세 정보
    private ItemDetailDto item;

    // 감정 정보
    private Long appraiseId;
    private Integer bidPrice;
    private AppraiseStatus status;
    private Boolean isSelected;
    private LocalDateTime createdAt;

    public static AppraiseAdminGetResponse from(Appraise appraise) {
        return new AppraiseAdminGetResponse(
                SellerDto.from(appraise.getItem().getSeller()),
                ItemDetailDto.from(appraise.getItem()),
                appraise.getId(),
                appraise.getBidPrice(),
                appraise.getAppraiseStatus(),
                appraise.isSelected(),
                appraise.getCreatedAt()
        );
    }

    // 내부 DTO들
    @Getter
    @AllArgsConstructor
    public static class ItemDetailDto {
        private Long itemId;
        private String name;
        private String description;
        private Long hopePrice;
        private String imageUrl;

        public static ItemDetailDto from(Item item) {
            return new ItemDetailDto(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getHopePrice(),
                    item.getImage()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SellerDto {
        private Long sellerId;
        private String email;
        private String sellerName;

        public static SellerDto from(User user) {
            return new SellerDto(
                    user.getId(),
                    user.getEmail(),
                    user.getName()
            );
        }
    }
}
