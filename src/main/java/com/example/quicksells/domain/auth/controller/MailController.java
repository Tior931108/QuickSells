package com.example.quicksells.domain.auth.controller;

import com.example.quicksells.common.model.CommonResponse;
import com.example.quicksells.domain.auth.model.request.AuthMailRequest;
import com.example.quicksells.domain.auth.model.request.AuthMailVerificationRequest;
import com.example.quicksells.domain.auth.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MailController {

    private final MailService mailService;

    /**
     * 인증번호 발송 메소드
     */
    @PostMapping("/mail")
    public ResponseEntity<CommonResponse> mailSend(@Valid @RequestBody AuthMailRequest request) {

        mailService.sendMail(request.getEmail());

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("인증번호가 발송되었습니다."));
    }

    /**
     * 인증번호 검증 메소드
     */
    @PostMapping("/verify-code")
    public ResponseEntity<CommonResponse> verifyCode(@Valid @RequestBody AuthMailVerificationRequest request) {

        boolean isVerified = mailService.verifyCode(request.getEmail(), request.getCode());

        if (!isVerified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.error("인증번호가 일치하지 않거나 만료되었습니다."));
        }

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.success("이메일 인증에 성공하였습니다."));
    }
}
