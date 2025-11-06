package com.sharecycle.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sharecycle.domain.MonthlySubscriberStrategy;
import com.sharecycle.domain.PayAsYouGoStrategy;

@RestController
@RequestMapping("/api/public/pricing")
public class PricingController {

    private final PayAsYouGoStrategy payAsYouGoStrategy = new PayAsYouGoStrategy();
    private final MonthlySubscriberStrategy monthlySubscriberStrategy = new MonthlySubscriberStrategy();

    @GetMapping("/info")
    public Map<String, String> getPricingInfo() {
        return Map.of(
            "payAsYouGo", payAsYouGoStrategy.displayInfo(),
            "monthlySubscriber", monthlySubscriberStrategy.displayInfo()
        );
    }
}