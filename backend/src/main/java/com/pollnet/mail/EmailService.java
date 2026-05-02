package com.pollnet.mail;

import com.pollnet.config.MailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Plain-text mail sender. When {@code pollnet.mail.enabled=false} (default in dev or
 * when SMTP isn't configured), it logs the message instead of attempting delivery —
 * keeps the verification/reset flows usable end-to-end without an SMTP provider.
 */
@Slf4j
@Service
public class EmailService {

    private final MailProperties props;
    private final JavaMailSender mailSender; // null when SMTP isn't configured

    public EmailService(MailProperties props, ObjectProvider<JavaMailSender> mailSender) {
        this.props = props;
        this.mailSender = mailSender.getIfAvailable();
    }

    public void send(String to, String subject, String body) {
        if (!props.enabled() || mailSender == null) {
            log.info("[mail-disabled] would send to={} subject={}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.from());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        try {
            mailSender.send(msg);
            log.info("Sent mail to={} subject={}", to, subject);
        } catch (Exception ex) {
            // Do not bubble: SMTP outage shouldn't make register/forgot-password 500.
            // The user can request a new token; we'll have alerts via the log line.
            log.error("Mail send failed to={} subject={} err={}", to, subject, ex.getMessage());
        }
    }
}
