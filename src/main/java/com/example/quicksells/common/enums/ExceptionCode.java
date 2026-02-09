package com.example.quicksells.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {

    // S3Service
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "최대 5MB까지 업로드 가능합니다."),
    ONLY_IMAGE_FILE(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"이미지 파일형식이 아닙니다."),

    //appraise
    NOT_FOUND_APPRAISE(HttpStatus.NOT_FOUND, "감정을 찾을 수 없습니다."), //auction
    NOT_FOUND_APPRAISER(HttpStatus.NOT_FOUND, "감정사를 찾을 수 없습니다."),
    NOT_APPRAISE_ITEM_DELETE(HttpStatus.CONFLICT, "삭제된 상품에는 감정을 할 수 없습니다."),
    EXISTS_ITEM_SELL(HttpStatus.CONFLICT, "이미 판매 완료된 상품입니다."),
    ONLY_OWNER_APPRAISE_SEARCH(HttpStatus.FORBIDDEN, "본인의 상품에 대한 감정만 조회할 수 있습니다."),
    NOT_DELETE_SELECTED_APPRAISE(HttpStatus.CONFLICT, "이미 선택된 감정은 삭제가 불가합니다."),
    EXISTS_ALREADY_SELECT_APPRAISE(HttpStatus.CONFLICT, "이미 선택된 다른 감정이 존재합니다."),
    ALREADY_SELECT_APPRAISE(HttpStatus.CONFLICT, "이미 선택된 감정입니다."),
    ALREADY_EXISTS_APPRAISE(HttpStatus.CONFLICT, "해당 상품에 이미 감정을 등록하셨습니다."),
    APPRAISE_NOT_SELECTED(HttpStatus.CONFLICT, "선택되지 않은 감정입니다."),
    APPRAISE_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 즉시판매/경매 진행중인 감정입니다."),
    FORBIDDEN_APPRAISE_ACCESS(HttpStatus.FORBIDDEN, "본인이 감정한 상품만 조회/수정할 수 있습니다."),
    CANNOT_UPDATE_SELECTED_APPRAISE(HttpStatus.CONFLICT, "이미 선택된 감정은 수정할 수 없습니다."),
    CANNOT_UPDATE_PROCESSED_APPRAISE(HttpStatus.CONFLICT, "이미 처리된 감정은 수정할 수 없습니다."),

    //auction
    NOT_FOUND_AUCTION(HttpStatus.NOT_FOUND, "경매 정보를 찾을 수 없습니다."),
    NOT_FOUND_DEAL(HttpStatus.NOT_FOUND, "거래를 찾을 수 없습니다."), //Auction, Deal
    BID_PRICE_TOO_LOW(HttpStatus.BAD_REQUEST, "현재 입찰가보다 더 높은 금액을 입력해야 합니다."),
    ACCESS_DENIED_ONLY_OWNER(HttpStatus.FORBIDDEN, "경매에 대한 권한이 없습니다."),
    AUCTION_ALREADY_EXPIRED(HttpStatus.BAD_REQUEST, "해당 경매가 종료되어 삭제되었습니다."),
    SELLER_CANNOT_PURCHASE_OWN_AUCTION(HttpStatus.FORBIDDEN, "판매자는 본인이 등록한 상품의 구매자가 될 수 없습니다."),

    //deal
    NOT_DEAL_ON_SALE(HttpStatus.NOT_FOUND, "거래 중 상태가 아닙니다."),//deal entity
    EXISTS_ACTIVE_DEAL(HttpStatus.CONFLICT, "이미 진행 중인 거래가 존재합니다."),
    ACCESS_DENIED_DEAL(HttpStatus.FORBIDDEN, "해당 거래에 대한 접근 권한이 없습니다."),

    //information
    NOT_FOUND_INFORMATION(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    EXISTS_INFORMATION_TITLE(HttpStatus.CONFLICT, "이미 존재하는 공지사항 제목입니다."),
    INFORMATION_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "공지사항 생성에 실패했습니다."),

    //auth
    NOT_FOUND_EMAIL(HttpStatus.NOT_FOUND, "이메일을 찾을 수 없습니다."),
    NOT_MATCHES_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    EXISTS_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    ALREADY_COMPLETED_SIGNUP(HttpStatus.CONFLICT, "이미 회원가입이 완료된 사용자입니다."),
    OAUTH_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 공급자입니다."),


    //USER
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."), //auction,Deal,User,Item
    NOT_FOUND_ADMIN(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),
    NOT_FOUND_TOKEN(HttpStatus.NOT_FOUND, "토큰을 찾을 수 없습니다."),
    EXISTS_PHONE(HttpStatus.CONFLICT, "이미 존재하는 핸드폰 번호입니다."), //auth,User
    NO_UPDATE_FIELD(HttpStatus.BAD_REQUEST, "수정할 정보가 없습니다."),
    INVALID_USER_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 권한입니다."), //UserRole
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "이전 비밀번호와 동일합니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    CONFLICT_AUCTION(HttpStatus.CONFLICT, "중복된 경매입니다."),
    CONFLICT_WISHLIST(HttpStatus.CONFLICT, "중복된 관심 목록입니다."),

    //ITEM
    NOT_FOUND_ITEM(HttpStatus.NOT_FOUND, "상품를 찾을 수 없습니다."), //appraise,Deal,item
    ACCESS_DENIED_EXCEPTION_UPDATED_ITEM(HttpStatus.FORBIDDEN, "상품 수정 권한이 없습니다."),
    ACCESS_DENIED_EXCEPTION_DELETED_ITEM(HttpStatus.FORBIDDEN, "상품 삭제 권한이 없습니다."),
    ACCESS_DENIED_EXCEPTION_GET_DETAIL_MY_ITEM(HttpStatus.FORBIDDEN, "상품 조회 권한이 없습니다."),
    CONFLICT_ITEM(HttpStatus.CONFLICT, "중복된 상품입니다."),
    ITEM_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "아이템 등록에 실패했습니다."),

    //AWS
    NOT_FOUND_FILE(HttpStatus.NOT_FOUND,"파일 이름이 존재하지 않습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드 가능합니다. (jpg, jpeg, png, gif)"),
    FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DELETE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다."),

    //ANSWER
    NOT_FOUND_ANSWER(HttpStatus.NOT_FOUND, "답변 내역을 찾을 수 없습니다."),
    ACCESS_DENIED_ANSWER(HttpStatus.FORBIDDEN, "조회할 권한이 없습니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 답변이 존재합니다."),

    //ASK
    NOT_FOUND_ASK(HttpStatus.NOT_FOUND, "문의 내역을 찾을 수 없습니다."),
    ONLY_OWNER_ASK(HttpStatus.NOT_FOUND, "본인의 문의 내역만 접근할 수 있습니다."),

    //SEARCH
    INVALID_SEARCH_KEYWORD(HttpStatus.NOT_FOUND, "검색어를 입력해주세요."),
    UNAUTHORIZED_SEARCH(HttpStatus.UNAUTHORIZED, "로그인 후 입력해주세요"),

    //PAYMENT
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "충전 금액은 최소 10,000원 이상이어야 합니다."),
    NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 올바르지 않습니다."),
    DUPLICATE_PAYMENT_KEY(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    NOT_FOUND_WALLET(HttpStatus.NOT_FOUND, "지갑 정보를 찾을 수 없습니다."),
    AUCTION_SETTLEMENT_PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "낙찰 정산 실패: 포인트가 부족합니다. 추가 충전 후 재정산이 필요합니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),

    /**
     * 토스 승인(confirm) 요청 자체가 실패한 경우
     * - 네트워크 오류, 인증 오류, 토스 측 검증 실패 등
     */
    TOSS_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "토스 결제 승인에 실패했습니다."),
    /**
     * 내부 DB 반영 실패 → 토스 cancel로 롤백 시도했는데 cancel까지 실패한 경우
     * - 이 경우는 '돈은 빠져나갔는데 DB 반영이 안됨' 같은 위험 상태가 될 수 있어
     *   빠르게 알람/로그로 잡아야 하는 심각도 높은 에러로 취급
     */
    TOSS_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "토스 결제 취소(롤백)에 실패했습니다."),

    //WISH_LIST
    ACCESS_DENIED_EXCEPTION_WISHLIST(HttpStatus.FORBIDDEN, "관심 목록 대한 권한이 없습니다."),
    NOT_EXIST_ONE_WISHLIST(HttpStatus.NOT_FOUND, "해당 관심 목록은 존재하지 않습니다."),
    SELF_WISH_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인 상품은 관심 목록으로 등록 할 수 없습니다."),

    //WebSocket & 채팅 & 토큰 및 principal 인증 관련
    MISSING_TOKEN(HttpStatus.UNAUTHORIZED, "유효한 토큰이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    CHAT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "채팅 권한이 없습니다."),
    PRINCIPAL_CHAT_PERMISSION_DENIED(HttpStatus.BAD_REQUEST, "본인과는 채팅할 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
    CHAT_BETWEEN_USERS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "일반 사용자는 구매자와 판매자 관계이며 경매 낙찰시 채팅 가능합니다."),
    NOT_FOUND_BUYER(HttpStatus.NOT_FOUND, "구매자 정보가 없습니다."),
    NOT_FOUND_SELLER(HttpStatus.NOT_FOUND, "판매자 정보가 없습니다."),
    NOT_MATCHED_CHAT_USER(HttpStatus.BAD_REQUEST, "해당 사용자는 이 채팅방의 참여자가 아닙니다"),
    INVALID_CHAT_ROOM_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 채팅방 타입입니다."),
    NOT_FOUND_PRINCIPAL(HttpStatus.NOT_FOUND, "인증 정보가 없습니다."),
    WRONG_PRINCIPAL_TYPE(HttpStatus.BAD_REQUEST, "잘못된 인증 타입입니다."),
    NOT_SAME_PRINCIPAL_AUTH_USER(HttpStatus.BAD_REQUEST, "Principal이 AuthUser 타입이 아닙니다"),


    //미사용
    AUCTION_EXPIRED_SOLD_OUT(HttpStatus.BAD_REQUEST, "경매 시간이 종료되어 낙찰이 완료 되었습니다."),
    AUCTION_EXPIRED_UNSOLD(HttpStatus.BAD_REQUEST, "경매시간이 종료되었으나 낙찰자가 없습니다."),
    NOT_DEAL_BEFORE(HttpStatus.NOT_FOUND, "거래 전 상태가 아닙니다."),
    ALREADY_DELETE_APPRAISE(HttpStatus.CONFLICT, "이미 삭제된 감정입니다."),
    NULL_POINT_BID_PRICE(HttpStatus.INTERNAL_SERVER_ERROR, "입찰 금액을 작성해주세요."),
    NOT_FOUND_MY_WISHLIST(HttpStatus.NOT_FOUND, "내 관심 목록을 찾을 수 없습니다."),
    BUYER_ID_REQUIRED_FOR_WISHLIST(HttpStatus.BAD_REQUEST, "관심목록 조회를 위해 구매자는 필수입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "현재 요청이 많아 처리에 실패했습니다. 잠시 후 다시 시도해주세요."),
    LOCK_INTERRUPTED_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 시스템 신호 간섭이 발생했습니다. 잠시 후 다시 시도해 주세요.")
    ;

    private final HttpStatus status;
    private final String message;

    ExceptionCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
