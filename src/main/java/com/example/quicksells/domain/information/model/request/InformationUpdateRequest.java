package com.example.quicksells.domain.information.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.repository.NoRepositoryBean;

@Getter
@AllArgsConstructor
@NoRepositoryBean
@Schema(description = "공지사항 수정")
public class InformationUpdateRequest {

    @Size(max = 50, message = "공지사항 제목은 최대 50자까지 입력할 수 있습니다.")
    @Schema(description = "공지사항 제목 수정")
    private String title;

    @Size(max = 500, message = "공지사항 내용은 최대 500자까지 입력할 수 있습니다.")
    @Schema(description = "공지사항 내용 수정")
    private String description;

    @Schema(description = "삭제할 이미지")
    private Boolean deleteImage;

    public boolean isAllFieldEmpty() {return title == null && description == null && deleteImage == null;}
}
