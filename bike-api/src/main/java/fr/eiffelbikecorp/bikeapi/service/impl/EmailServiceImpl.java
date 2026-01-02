package fr.eiffelbikecorp.bikeapi.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendEmail(String to, String subject, String content) {
        try {
            log.info("Starting email send to {}", to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@bike.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("email successfully sent to {}", to);
        } catch (MailException e) {
            log.error("FAILED to send email to {}. Error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}", to, e);
        }
    }
}