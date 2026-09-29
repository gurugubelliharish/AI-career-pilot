package com.aicareer.jobradar.module.notification.service;

import com.aicareer.jobradar.module.auth.model.User;
import com.aicareer.jobradar.module.matching.model.MatchResult;
import com.aicareer.jobradar.module.notification.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TelegramBot telegramBot;
    private final JavaMailSender mailSender;

    @Async
    public void notifyHighPriorityJob(User user, MatchResult match) {
        String message = buildJobMessage(match);

        if (user.isTelegramNotificationsEnabled() && user.getTelegramChatId() != null) {
            sendTelegram(user.getTelegramChatId(), message);
        }

        if (user.isEmailNotificationsEnabled()) {
            sendEmail(user.getEmail(), "New " + match.getPriority() + " Priority Job: " + match.getJobTitle(), message);
        }
    }

    private void sendTelegram(String chatId, String message) {
        try {
            telegramBot.sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Telegram notification failed for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Email notification failed for {}: {}", to, e.getMessage());
        }
    }

    private String buildJobMessage(MatchResult match) {
        return """
                🚀 New %s Priority Job Found!

                Role:     %s
                Company:  %s
                Match:    %.0f%%
                Posted:   %s

                Priority Score: %.0f

                ✅ Matched Skills: %s
                ❌ Missing Skills: %s

                Open AI Job Radar to review and apply.
                """.formatted(
                match.getPriority(),
                match.getJobTitle(),
                match.getCompany(),
                match.getMatchScore(),
                match.getPostedAgo(),
                match.getPriorityScore(),
                String.join(", ", match.getMatchedSkills()),
                String.join(", ", match.getMissingSkills()));
    }
}
