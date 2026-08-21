package com.sebu.backend.mypage.moderation;

import org.springframework.stereotype.Component;

@Component
public class IntroductionModeratorImpl implements IntroductionModerator {

    @Override
    public ModerationResult moderate(String introduction) {
        if (introduction == null || introduction.isBlank()) {
            return new ModerationResult(
                    true,
                    "v1",
                    "internal"
            );
        }

        return new ModerationResult(
                true,
                "v1",
                "internal"
        );
    }
}
