

package com.sharecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sharecycle")

public class SharecycleApplication {

  public static void main(String[] args) {
    SpringApplication.run(SharecycleApplication.class, args);
  }
}
