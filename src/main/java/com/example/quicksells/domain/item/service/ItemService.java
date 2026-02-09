package com.example.quicksells.domain.item.service;

import com.example.quicksells.common.aws.service.S3Service;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auth.model.dto.AuthUser;
import com.example.quicksells.domain.item.model.request.ItemCreatedRequest;
import com.example.quicksells.domain.item.model.request.ItemUpdateRequest;
import com.example.quicksells.domain.item.model.response.ItemCreatedResponse;
import com.example.quicksells.domain.item.model.response.ItemGetDetailResponse;
import com.example.quicksells.domain.item.model.response.ItemGetListResponse;
import com.example.quicksells.domain.item.model.response.ItemUpdateResponse;
import com.example.quicksells.domain.item.entity.Item;
import com.example.quicksells.domain.item.repository.ItemRepository;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    /**
     * 상품 생성 로직
     *
     * @param authUser 상품 생성에 필요한 데이터 요청
     * @param request  생성된 상품 정보 응답하는 DTO
     * @return
     */
    @Transactional
    public ItemCreatedResponse createItem(AuthUser authUser, ItemCreatedRequest request, MultipartFile itemImage) {
        //유저(판매자) 조회
        User seller = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        //중복 상품 검증
        boolean exists = itemRepository.existsBySellerIdAndName(authUser.getId(), request.getName());

        //상품 중복 등록 시 409에러 발생
        if (exists) {
            throw new CustomException(ExceptionCode.CONFLICT_ITEM);
        }

        //이미지 체크 -> DB 검증 후 업로드 수행
        String imageUrl = uploadImageIfPresent(itemImage);

        try {
            // 엔티티 생성
            Item item = new Item(seller, request.getName(), request.getHopePrice(), request.getDescription(), imageUrl);

            // Item 엔티티 DB에 저장
            Item saved = itemRepository.save(item);

            //응답 전용 객체
            return ItemCreatedResponse.from(saved);

        } catch (Exception e) {

            // DB 저장 실패 시 업호드된 이미지 삭제
            if (imageUrl != null) {
                s3Service.deleteImage(imageUrl);
            }
            throw new CustomException(ExceptionCode.ITEM_CREATE_FAILED);
        }
    }

    /**
     * 상품 상세 조회 (관리자)
     *
     * @param id 상품 조회 ID
     * @return 상품 상세 정보 응답 DTO
     */
    @Transactional(readOnly = true)
    public ItemGetDetailResponse getDetailItem(Long id) {

        //상품 조회 및 검증 404에러
        Item item = itemRepository.findItemDetail(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ITEM));

        // 조회된 엔티티 -> DTO로 변환
        return ItemGetDetailResponse.from(item);
    }

    /**
     * (페이징) 상품 목록 조회 (관리자)
     *
     * @param pageable 페이지, 사이즈 정보
     * @return 페이징된 상품 목록 응답 DTO
     */
    @Transactional(readOnly = true)
    public Page<ItemGetListResponse> getAll(Pageable pageable) {

        //상품 목록 조회 -> QueryDsl 적용
        Page<Item> result = itemRepository.findItemList(pageable);

        //맵 사용하여 엔티티 목록 -> 응답 DTO 목록으로 조회
        return result.map(ItemGetListResponse::from);
    }

    /**
     * 나의 상품 상세 조회
     *
     * @param id       상품 조회 ID
     * @param authUser 로그인한 유저 ID
     * @return 상품 상세 조회 응답 DTO
     */
    @Transactional(readOnly = true)
    public ItemGetDetailResponse getMyDetail(Long id, AuthUser authUser) {
        //상품 조회 아닌 경우 404에러
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ITEM));

        //상품 조회 후 내 상품이 맞는지 확인 아닌경우 403에러
        if (!item.getSeller().getId().equals(authUser.getId())) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED_EXCEPTION_GET_DETAIL_MY_ITEM);
        }
        return ItemGetDetailResponse.from(item);
    }

    /**
     * 나의 상품 목록 조회
     *
     * @param authUser 로그인한 유저 ID
     * @param pageable 페이징 적용된 목록 조회
     * @return 페이징 적용된 상품 목록 조회 응답 DTO
     */
    @Transactional(readOnly = true)
    public Page<ItemGetListResponse> getMyItemList(AuthUser authUser, Pageable pageable) {
        //유저 조회
        User seller = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        //판매자 본인이 등록한 아이템 목록 확인
        Page<Item> myItems = itemRepository.findAllBySeller(seller, pageable);

        return myItems.map(ItemGetListResponse::from);
    }

    /**
     * 상품 수정 기능
     *
     * @param authUser 로그인한 사용자 정보
     * @param id       수정하려는 상품 IDr
     * @param request  수정할 상품 정보(이름, 희망가격, 설명, 이미지)
     * @return 수정된 상품 정보를 담은 응답 DTO
     */
    @Transactional
    public ItemUpdateResponse updateItem(AuthUser authUser, Long id, ItemUpdateRequest request, MultipartFile itemImage) {

        //상품 조회 - 존재 안할 시 404에러
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ITEM));

        //상품 작성자와 로그인한 회원이 다르면 수정 불가
        if (!item.getSeller().getId().equals(authUser.getId())) {
            throw new CustomException(ExceptionCode.ACCESS_DENIED_EXCEPTION_UPDATED_ITEM);
        }

        //이름 중복 시 에러 409에러 발생
        if (request.getName() != null && !request.getName().equals(item.getName())) {

            if (itemRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new CustomException(ExceptionCode.CONFLICT_ITEM);
            }
        }

        //이미지 수정
        String finalImageUrl = itemImageUpdate(item, itemImage);

        //수정메소드 불러오기
        item.update(request.getName(), request.getHopePrice(), request.getDescription(), finalImageUrl);

        //수정 결과 DTO로 변환하여 반환
        return ItemUpdateResponse.from(item);
    }

    /**
     * 상품 삭제 기능
     *
     * @param id       삭제하려는 상품 ID
     * @param authUser 로그안한 사용자 정보
     */
    @Transactional
    public void deleteItem(Long id, AuthUser authUser) {
        //삭제 대상 상품 조회
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_ITEM));

        // 상품 작성자와 로그인한 회원 정보 다르면 삭제 불가
        if (!item.getSeller().getId().equals(authUser.getId())) {

            throw new CustomException(ExceptionCode.ACCESS_DENIED_EXCEPTION_DELETED_ITEM);
        }

        //소프트 삭제
        item.softDelete();
    }

    /**
     * 이미지 업로드 여부 체크 기능
     *
     * @param itemImage 업로드할 이미지 파일
     * @return 업로드된 이미지
     */
    // 이미지 체크 기능
    private String uploadImageIfPresent(MultipartFile itemImage) {

        //1. 이미지가 없는 경우 null 반환
        if (itemImage == null || itemImage.isEmpty()) {
            return null;
        }

        //2. 이미지 있는 경우 s3 업로드 메서드에 전달
        return s3Service.uploadImage(itemImage);
    }

    /**
     * 상품 이미지 수정 로직
     *
     * @param item      수정할 상품
     * @param itemImage 상픔에 사용될 수정할 이미지 파일
     * @return 최종 저장될 이미지
     */
    //수정된 이미지 업로드
    private String itemImageUpdate(Item item, MultipartFile itemImage) {
        //데이터 불러오기
        String oldImage = item.getImage();

        //새 이미지 없는 경우 -> 기존 유지
        if (itemImage == null || itemImage.isEmpty()) {
            return oldImage;
        }

        //새 이미지 업로드(수정 먼저)
        String newImage = uploadImageIfPresent(itemImage);

        try {
            //업로드 성공 후 기존 이미지 삭제
            if (newImage != null && oldImage != null && !oldImage.isBlank()) {
                s3Service.deleteImage(oldImage);
            }
        } catch (Exception e) {

        }

        //새 이미지 URL 반환
        return newImage;
    }
}
