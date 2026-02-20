package com.example.quicksells.domain.search.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TrendingKeyword {
    private String keyword;
    private Long recentCount;
    private Long previousCount;
    private Double growthRate;

}
