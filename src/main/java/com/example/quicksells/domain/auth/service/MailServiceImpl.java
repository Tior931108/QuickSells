package com.example.quicksells.domain.auth.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.user.entity.User;
import com.example.quicksells.domain.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 멀티스레드 환경을 위해 ConcurrentHashMap 사용
    private final Map<String, Integer> verificationCodes = new ConcurrentHashMap<>();

    private static final String SENDER_EMAIL = "jeonghyeogc1@gmail.com";
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBER = "0123456789";
    private static final String OTHER_CHAR = "!@#$%^&*()_+-=[]?";
    private static final String PASSWORD_ALLOW_BASE = CHAR_LOWER + CHAR_UPPER + NUMBER + OTHER_CHAR;
    private static final SecureRandom random = new SecureRandom();

    /**
     * 인증 코드 전송
     */
    @Override
    public void sendCodeMail(String email) {

        int code = createNumber(email);

        String title = "[Quick-Sells] 이메일 인증번호";
        String content = "<h3>Quick-Sells 이메일 인증번호</h3>" +
                "<h1 style='color:#4F46E5;'>" + code + "</h1>";

        sendMail(email, title, content);
    }

    /**
     * 인증 코드 검증
     */
    @Override
    public boolean verifyCode(String email, int code) {

        Integer storedCode = verificationCodes.get(email);

        if (storedCode != null && storedCode == code) {
            verificationCodes.remove(email);
            return true;
        }
        return false;
    }

    /**
     * 임시 발급 비밀번호 전송
     */
    @Override
    public void sendTemporaryPasswordMail(String email, String tempPassword) {

        String title = "[Quick-Sells] 임시 발급 비밀번호";
        String content = "<h3>Quick-Sells 임시 발급 비밀번호</h3>" +
                "<h1 style='color:#4F46E5;'>" + tempPassword + "</h1>";

        sendMail(email, title, content);
    }

    /**
     * 임시 비밀번호 발급 후 DB 저장
     */
    @Override
    @Transactional
    public String createTemporaryPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        String temporaryPassword = createRandomPassword();

        user.updatePassword(passwordEncoder.encode(temporaryPassword));

        return temporaryPassword;
    }

    /**
     * 임시 발급 비밀번호 검증
     */
    @Override
    public boolean verifyTemporaryPassword(String email, String tempPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_USER));

        return passwordEncoder.matches(tempPassword, user.getPassword());
    }


    // 인증 코드 생성 및 저장
    private int createNumber(String email) {

        int number = new Random().nextInt(900000) + 100000;
        verificationCodes.put(email, number);
        return number;
    }

    // 임시 비밀번호 생성
    private String createRandomPassword() {
        int length = 10;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int rndCharAt = random.nextInt(PASSWORD_ALLOW_BASE.length());
            char rndChar = PASSWORD_ALLOW_BASE.charAt(rndCharAt);

            sb.append(rndChar);
        }

        return sb.toString();
    }

    // 이메일 발송
    private void sendMail(String email, String title, String htmlContent) {

        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(SENDER_EMAIL);
            helper.setTo(email);
            helper.setSubject(title);

            String fullBody =
                    "<div style='font-family: Arial, sans-serif; text-align:center;'>" +
                            "  <img src='https://velog.velcdn.com/images/fluxing/post/334a1bca-3b27-4715-9126-adf58d16262c/image.png' " +
                            "       style='width:100%; max-width:320px; margin-bottom:24px;' />" +
                            htmlContent +
                            "  <p style='margin-top:20px;'>감사합니다.</p>" +
                            "</div>";

            helper.setText(fullBody, true);
            javaMailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("메일 발송 중 오류가 발생했습니다.", e);
        }
    }
}
