

package com.sharecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sharecycle")
@EnableScheduling
public class SharecycleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SharecycleApplication.class, args);
  }
}
