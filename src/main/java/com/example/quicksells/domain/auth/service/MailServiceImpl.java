package com.example.quicksells.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    private static final String SENDER_EMAIL = "jeonghyeogc1@gmail.com";

    // 멀티스레드 환경을 위해 ConcurrentHashMap 사용
    private final Map<String, Integer> verificationCodes = new ConcurrentHashMap<>();

    // 메일 생성
    @Override
    public MimeMessage createMail(String mail) {

        int code = createNumber(mail);
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(SENDER_EMAIL);
            helper.setTo(mail);
            helper.setSubject("[Quick-Sells] 이메일 인증번호");
            String body =
                    "<div style='font-family: Arial, sans-serif; text-align:center;'>" +
                            "  <img src='https://velog.velcdn.com/images/fluxing/post/334a1bca-3b27-4715-9126-adf58d16262c/image.png' " +
                            "       style='width:100%; max-width:320px; margin-bottom:24px;' />" +
                            "  <h3>Quick-Sells 이메일 인증번호</h3>" +
                            "  <h1 style='color:#4F46E5;'>" + code + "</h1>" +
                            "  <p style='margin-top:20px;'>감사합니다.</p>" +
                            "</div>";
            helper.setText(body, true);
        } catch (MessagingException e) {
            throw new RuntimeException("메일 생성 중 오류가 발생했습니다.", e);
        }

        return message;
    }

    // 인증 코드 보내기
    @Override
    public CompletableFuture<Integer> sendMail(String mail) {
        MimeMessage message = createMail(mail);
        javaMailSender.send(message);
        return CompletableFuture.completedFuture(verificationCodes.get(mail));
    }

    // 인증 코드 검증
    @Override
    public boolean verifyCode(String email, int code) {
        Integer storedCode = verificationCodes.get(email);

        if (storedCode != null && storedCode == code) {
            verificationCodes.remove(email); // 인증 성공 시 삭제 (일회용)
            return true;
        }
        return false;
    }

    // 인증 코드 생성 및 저장
    private int createNumber(String email) {

        int number = new Random().nextInt(900000) + 100000;
        verificationCodes.put(email, number);
        return number;
    }
}
