package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.request.AuthMailRequest;
import com.example.quicksells.domain.auth.model.request.AuthMailCodeVerificationRequest;
import com.example.quicksells.domain.auth.model.request.AuthPasswordVerificationRequest;
import com.example.quicksells.domain.auth.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 이메일(email) 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MailController {

    private final MailService mailService;

    /**
     * 인증번호 발송 메소드
     */
    @Operation(summary = "이메일 인증번호 발송")
    @PostMapping("/emails/verification")
    public ResponseEntity<CommonResponse> mailSend(@Valid @RequestBody AuthMailRequest request) {

        mailService.sendCodeMail(request.getEmail());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("인증번호가 발송되었습니다."));
    }

    /**
     * 인증번호 검증 메소드
     */
    @Operation(summary = "이메일 인증번호 검증")
    @PostMapping("/emails/verification/verify")
    public ResponseEntity<CommonResponse> verifyCode(@Valid @RequestBody AuthMailCodeVerificationRequest request) {

        boolean isVerified = mailService.verifyCode(request.getEmail(), request.getCode());

        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.error("인증번호가 일치하지 않거나 만료되었습니다."));
        }

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("이메일 인증에 성공하였습니다."));
    }

    /**
     * 임시 비밀번호 재발급 발송 메서드
     */
    @Operation(summary = "임시 비밀번호 재발급 이메일 발송")
    @PostMapping("/passwords/reset")
    public ResponseEntity<CommonResponse> resetPassword(@Valid @RequestBody AuthMailRequest mailRequest) {

        String tempPassword = mailService.createTemporaryPassword(mailRequest.getEmail());

        mailService.sendTemporaryPasswordMail(mailRequest.getEmail(), tempPassword);

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("임시 비밀번호가 이메일로 발송되었습니다."));
    }

    /**
     * 임시 비밀번호 검증 메소드
     */
    @Operation(summary = "임시 비밀번호 검증")
    @PostMapping("/passwords/verify")
    public ResponseEntity<CommonResponse> verifyTemporaryPassword(@Valid @RequestBody AuthPasswordVerificationRequest request) {

        boolean isVerified = mailService.verifyTemporaryPassword(request.getEmail(), request.getTemporaryPassword());

        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.error("임시 비밀빈호가 일치하지 않습니다."));
        }

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("임시 비밀번호 인증 성공하였습니다."));
    }
}