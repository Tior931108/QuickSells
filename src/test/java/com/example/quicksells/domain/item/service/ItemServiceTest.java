package com.example.quicksells.domain.item.service;

import com.example.quicksells.common.aws.service.S3Service;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.item.model.request.ItemCreatedRequest;
import com.example.quicksells.domain.item.model.request.ItemUpdateRequest;
import com.example.quicksells.domain.item.model.response.ItemCreatedResponse;
import com.example.quicksells.domain.item.model.response.ItemGetDetailResponse;
import com.example.quicksells.domain.item.model.response.ItemGetListResponse;
import com.example.quicksells.domain.item.model.response.ItemUpdateResponse;
import com.example.quicksells.domain.item.repository.ItemRepository;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.quicksells.common.enums.UserRole.USER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @InjectMocks //테스트 대상
    private ItemService itemService;

    @Mock //서비스가 의존하는 것들
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    private AuthUser authUser;

    @BeforeEach
// 공통데이터 초기화
    void setUp() {
        authUser = new AuthUser(1L, "test@test.com", USER, "name");
    }

    @Test
    @DisplayName("상품 생성 성공")
    void itemCreated() {

        //given
        ItemCreatedRequest request = new ItemCreatedRequest("상품1", 1L, "상품설명");
        User seller = mock(User.class);

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(s3Service.uploadImage(image)).thenReturn("img-url");

        Item saved = new Item(seller, "상품1", 1L, "상품설명", "img_url");
        when(itemRepository.save(any(Item.class))).thenReturn(saved);

        //when
        ItemCreatedResponse response = itemService.createItem(authUser, request, image);

        //then
        assertThat(response).isNotNull();
        verify(s3Service).uploadImage(image);
        verify(s3Service, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("상품 생성 실패 - 중복된 상품")
    void itemCreated_fail_existsItem() {

        // given
        ItemCreatedRequest request = new ItemCreatedRequest("상품1", 1L, "상품설명");
        User seller = mock(User.class);

        MultipartFile image = mock(MultipartFile.class);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(itemRepository.existsBySellerIdAndName(authUser.getId(), request.getName())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> itemService.createItem(authUser, request, image))
                .isInstanceOf(CustomException.class)
                .hasMessage("중복된 상품입니다.");
    }

    @Test
    @DisplayName("상품 생성 실패 - 저장 중 예외 발생 시 이미지 삭제")
    void itemCreated_fail_exception_deleteImage() {

        // given
        ItemCreatedRequest request = new ItemCreatedRequest("상품1", 1L, "상품설명");
        User seller = mock(User.class);

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(s3Service.uploadImage(image)).thenReturn("img-url");

        doThrow(new RuntimeException("DB 오류"))
                .when(itemRepository)
                .save(any(Item.class));

        // when & then
        assertThatThrownBy(() ->
                itemService.createItem(authUser, request, image))
                .isInstanceOf(CustomException.class)
                .hasMessage("아이템 등록에 실패했습니다.");

        verify(s3Service).uploadImage(image);
        verify(s3Service).deleteImage("img-url");
    }

    @Test
    @DisplayName("상품 생성 실패 - 이미지 없으면 삭제 로직 실행되지 않음")
    void itemCreated_fail_exception_withoutImage_noDelete() {

        // given
        ItemCreatedRequest request = new ItemCreatedRequest("상품1", 1L, "상품설명");
        User seller = mock(User.class);

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

        doThrow(new RuntimeException("DB 오류"))
                .when(itemRepository)
                .save(any(Item.class));

        when(image.isEmpty()).thenReturn(true);

        // when & then
        assertThatThrownBy(() ->
                itemService.createItem(authUser, request, image))
                .isInstanceOf(CustomException.class)
                .hasMessage("아이템 등록에 실패했습니다.");

        verify(s3Service, never()).uploadImage(any());
        verify(s3Service, never()).deleteImage(any());
    }

    @Test
    @DisplayName("상품 상세 조회 (관리자) 성공")
    void itemGetDetail_success() {

        // given
        Long id = 1L;

        Item item = mock(Item.class);
        User seller = mock(User.class);

        when(itemRepository.findItemDetail(id)).thenReturn(Optional.of(item));
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(1L);

        // when
        ItemGetDetailResponse response = itemService.getDetailItem(id);

        // then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("상품 목록 조회 (관리자) 성공")
    void itemGetAll_success() {

        // given
        User seller = mock(User.class);

        Item item1 = new Item(seller, "상품1", 1L, "상품설명1", "img_url2");
        Item item2 = new Item(seller, "상품1", 2L, "상품설명2", "img_url2");
        List<Item> itemList = Arrays.asList(item1, item2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> itemPage = new PageImpl<>(itemList, pageable, itemList.size());

        when(itemRepository.findItemList(pageable)).thenReturn(itemPage);

        // when
        Page<ItemGetListResponse> responses = itemService.getAll(pageable);

        // then
        assertEquals(10, responses.getSize());
        verify(itemRepository).findItemList(pageable);
    }


    @Test
    @DisplayName("나의 등록 상품 상세 조회 성공")
    void getMyDetailItem_success() {

        //given
        Long id = 10L;

        Item item = mock(Item.class);
        User seller = mock(User.class);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(1L);

        //when
        ItemGetDetailResponse response = itemService.getMyDetail(id, authUser);

        //then
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("나의 등록 상품 상세 조회 실패 - 내 상품이아닌 경우")
    void getMyDetailItem_fail() {

        // given
        Long id = 1L;

        Item item = mock(Item.class);

        User seller = mock(User.class);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> itemService.getMyDetail(id, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("상품 조회 권한이 없습니다.");
    }

    @Test
    @DisplayName("나의 등록 상품 목록 조회 성공")
    void getItemList_success() {

        // given
        User seller = mock(User.class);

        Item item1 = new Item(seller, "상품1", 1L, "상품설명1", "img_url2");
        Item item2 = new Item(seller, "상품1", 2L, "상품설명2", "img_url2");
        List<Item> itemList = Arrays.asList(item1, item2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Item> itemPage = new PageImpl<>(itemList, pageable, itemList.size());

        when(userRepository.findById(authUser.getId())).thenReturn(Optional.of(seller));
        when(itemRepository.findAllBySeller(seller, pageable)).thenReturn(itemPage);

        // when
        Page<ItemGetListResponse> responses = itemService.getMyItemList(authUser, pageable);

        // then
        assertEquals(10, responses.getSize());
        verify(itemRepository).findAllBySeller(seller, pageable);
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateItem_success() {

        // given
        Long id = 1L;

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        String oldImageUrl = "imageUrl";

        Item item = new Item(seller, "상품", 1L, "상품설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("수정 상품명", 200L, "수정 상품 설명", false);

        MultipartFile image = mock(MultipartFile.class);
        String newImageUrl = "newImageUrl";

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(image.isEmpty()).thenReturn(false);
        when(s3Service.uploadImage(image)).thenReturn(newImageUrl);
        when(itemRepository.existsByNameAndIdNot(request.getName(), id)).thenReturn(false);

        // when
        ItemUpdateResponse response = itemService.updateItem(authUser, id, request, image);

        // then
        assertThat(item.getName()).isEqualTo("수정 상품명");
        assertThat(item.getHopePrice()).isEqualTo(200L);
        assertThat(item.getDescription()).isEqualTo("수정 상품 설명");
        assertThat(item.getImage()).isEqualTo(newImageUrl);

        verify(s3Service).uploadImage(image);
        verify(s3Service).deleteImage(oldImageUrl);

        assertThat(response.getUserId()).isEqualTo(authUser.getId());
    }

    @Test
    @DisplayName("상품 수정 실패 - 작성자와 로그인한 회원 불일치")
    void updateItem_fail_differentUser() {

        // given
        Long id = 1L;

        User seller = mock(User.class);
        ItemUpdateRequest request = mock(ItemUpdateRequest.class);
        MultipartFile file = mock(MultipartFile.class);

        Item item = mock(Item.class);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> itemService.updateItem(authUser, id, request, file))
                .isInstanceOf(CustomException.class)
                .hasMessage("상품 수정 권한이 없습니다.");
    }

    @Test
    @DisplayName("상품 수정 실패 - 상품 이름 중복")
    void updateItem_fail_same_ItemName() {

        // given
        Long id = 1L;

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 1L, "상품설명", "imageUrl");

        ItemUpdateRequest request = new ItemUpdateRequest("동일 이름", 200L, "수정 상품 설명", false);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.existsByNameAndIdNot(request.getName(), id)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> itemService.updateItem(authUser, id, request, null))
                .isInstanceOf(CustomException.class)
                .hasMessage("중복된 상품입니다.");
    }

    @Test
    @DisplayName("상품 수정 - 이름이 null이면 중복 체크를 건너뜀")
    void updateItem_nameIsNull() {

        // given
        Long id = 1L;

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 1L, "상품설명", "imageUrl");

        ItemUpdateRequest request = new ItemUpdateRequest(null, 200L, "설명", false);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, null);

        // then
        verify(itemRepository, never()).existsByNameAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("상품 수정 - 이름이 기존과 같으면 중복 체크를 건너뜀")
    void updateItem_nameIsSameAsOriginal() {

        // given
        Long id = 1L;

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        String sameName = "기존상품 이름";
        Item item = new Item(seller, sameName, 100L, "설명", "imageUrl");
        ItemUpdateRequest request = new ItemUpdateRequest(sameName, 200L, "설명", false);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, null);

        // then
        verify(itemRepository, never()).existsByNameAndIdNot(anyString(), anyLong());
    }

    @Test
    @DisplayName("상품 이미지 수정 - 새 이미지가 null이면 기존 이미지를 유지")
    void itemImageUpdate_keepOldImageUrl_WhenNull() {

        // given
        Long id = 1L;

        String oldImageUrl = "oldImageUrl";

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 100L, "설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("상품", 100L, "설명", false);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, null);

        // then
        assertThat(item.getImage()).isEqualTo(oldImageUrl);
        verify(s3Service, never()).uploadImage(any());
        verify(s3Service, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("상품 이미지 수정 - 새 이미지가 Empty이면 기존 이미지를 유지")
    void itemImageUpdate_keepOldImageUrl_whenEmpty() {

        // given
        Long id = 1L;

        String oldImageUrl = "oldImageUrl";

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 100L, "설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("상품", 100L, "설명", false);

        MultipartFile emptyImage = mock(MultipartFile.class);

        when(emptyImage.isEmpty()).thenReturn(true);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, emptyImage);

        // then
        assertThat(item.getImage()).isEqualTo(oldImageUrl);
        verify(s3Service, never()).uploadImage(any());
    }

    @Test
    @DisplayName("상품 이미지 수정 - 새 이미지가 있고 기존 이미지도 있으면 업로드 후 기존 이미지를 삭제")
    void itemImageUpdate_replaceSuccess() {

        // given
        Long id = 1L;
        String oldImageUrl = "oldImageUrl";
        String newImageUrl = "newImageUrl";

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 100L, "설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("상품", 100L, "설명", false);

        MultipartFile newImageFile = mock(MultipartFile.class);

        when(newImageFile.isEmpty()).thenReturn(false);
        when(s3Service.uploadImage(newImageFile)).thenReturn(newImageUrl);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, newImageFile);

        // then
        assertThat(item.getImage()).isEqualTo(newImageUrl);
        verify(s3Service).deleteImage(oldImageUrl);
    }

    @Test
    @DisplayName("상품 이미지 수정 - 기존 이미지가 없는 상태에서 새 이미지만 추가")
    void itemImageUpdate_firstUpload() {

        // given
        Long id = 1L;

        String oldImageUrl = null;
        String newImageUrl = "new-image-url";

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 100L, "설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("상품", 100L, "설명", false);

        MultipartFile newImageFile = mock(MultipartFile.class);

        when(newImageFile.isEmpty()).thenReturn(false);
        when(s3Service.uploadImage(newImageFile)).thenReturn(newImageUrl);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.updateItem(authUser, id, request, newImageFile);

        // then
        assertThat(item.getImage()).isEqualTo(newImageUrl);
        verify(s3Service, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("상품 이미지 수정 - S3 기존 이미지 삭제 중 예외가 발생해도 프로세스는 계속 진행")
    void itemImageUpdate_deleteImageFailButContinue() {

        // given
        Long id = 1L;
        String oldImageUrl = "oldImageUrl";
        String newImageUrl = "newImageUrl";

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(authUser.getId());

        Item item = new Item(seller, "상품", 100L, "설명", oldImageUrl);

        ItemUpdateRequest request = new ItemUpdateRequest("상품", 100L, "설명", false);

        MultipartFile newImageFile = mock(MultipartFile.class);

        when(newImageFile.isEmpty()).thenReturn(false);
        when(s3Service.uploadImage(newImageFile)).thenReturn(newImageUrl);

        doThrow(new RuntimeException("S3 Delete Error")).when(s3Service).deleteImage(oldImageUrl);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when & then
        assertDoesNotThrow(() -> itemService.updateItem(authUser, id, request, newImageFile));
        assertThat(item.getImage()).isEqualTo(newImageUrl);
    }

    @Test
    @DisplayName("나의 등록 상품 삭제 성공")
    void deleteItem_success() {

        // given
        Long id = 1L;
        Long userId = 1L;

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(userId);

        Item item = new Item (seller, "상품1", 1L, "상품설명", "img_url");

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        // when
        itemService.deleteItem(id, authUser);

        // then
        assertThat(item.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("나의 등록 상품 삭제 실패 - 상품 등록 작성자가 아닐 때")
    void deleteItem_fail() {

        // given
        Long id = 1L;

        Item item = mock(Item.class);

        User seller = mock(User.class);

        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(item.getSeller()).thenReturn(seller);
        when(seller.getId()).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> itemService.deleteItem(id, authUser))
                .isInstanceOf(CustomException.class)
                .hasMessage("상품 삭제 권한이 없습니다.");
    }
}