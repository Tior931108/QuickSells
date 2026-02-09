package com.example.quicksells.domain.item.model.dto;

import com.example.quicksells.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ItemInfoDto {

    private final String name;
    private final Long hopePrice;
    private final String description;
    private final String image;
    private final UserRole role;
    private final LocalDateTime createdAt;

}
