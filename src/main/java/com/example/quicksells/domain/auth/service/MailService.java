package com.example.quicksells.domain.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public interface MailService {

    MimeMessage createMail(String mail);

    boolean verifyCode(String email, int code);

    CompletableFuture<Integer> sendMail(String mail);
}
