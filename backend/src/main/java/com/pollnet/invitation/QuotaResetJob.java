package com.pollnet.invitation;

import com.pollnet.config.InviteProperties;
import com.pollnet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaResetJob {

    private final UserRepository userRepository;
    private final InviteProperties props;

    /** Every month on the 1st at 00:00 (server timezone). */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthly() {
        int updated = userRepository.resetAllInviteQuotas(props.quotaMonthly());
        log.info("Monthly invite-quota reset: {} users → {}", updated, props.quotaMonthly());
    }
}
