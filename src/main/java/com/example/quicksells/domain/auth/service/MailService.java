package com.example.quicksells.domain.auth.service;

import org.springframework.stereotype.Service;

@Service
public interface MailService {

    // 인증 번호
    void sendCodeMail(String email);

    boolean verifyCode(String email, int code);

    // 임시 비밀번호
    String createTemporaryPassword(String email);

    void sendTemporaryPasswordMail(String email, String tempPassword);

    boolean verifyTemporaryPassword(String email, String tempPassword);
}
