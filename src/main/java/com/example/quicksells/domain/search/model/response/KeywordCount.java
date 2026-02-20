package com.example.quicksells.domain.search.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCount {
    private String keyword;
    private Long count;
    private Long lastSearchTime;
}
