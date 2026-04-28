package com.klu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String mailFromOverride;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendPlainText(String to, String subject, String body) {
        if (!mailEnabled) {
            return;
        }
        if (mailSender == null) {
            log.warn("app.mail.enabled is true but mail is not configured (e.g. spring.mail.host). Skipping email to {}.", to);
            return;
        }
        if (!StringUtils.hasText(to)) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            String from = StringUtils.hasText(mailFromOverride) ? mailFromOverride : mailUsername;
            if (StringUtils.hasText(from)) {
                msg.setFrom(from);
            }
            mailSender.send(msg);
        } catch (MailException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}
