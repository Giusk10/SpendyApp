package com.spendy.auth;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;;

@SpringBootApplication(scanBasePackages = "com.spendy.auth")
@ApplicationPath("/rest")
public class AuthMicroServiceApplication extends ResourceConfig
{
    public AuthMicroServiceApplication()
    {
        packages("com/spendy/auth/Controller");
    }

    public static void main(String[] args)
    {
        SpringApplication.run(AuthMicroServiceApplication.class, args);
    }
}
