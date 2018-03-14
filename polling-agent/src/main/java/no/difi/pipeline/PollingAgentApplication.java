package no.difi.pipeline;

import jdk.incubator.http.HttpClient;
import no.difi.pipeline.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

@SpringBootApplication
public class PollingAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(PollingAgentApplication.class);

    public static void main(String...args) {
        setDefaultUncaughtExceptionHandler((t, e) -> {
                    logger.error("Thread " + t.getName() + " failed - exiting", e);
                    System.exit(1);
                });
        SpringApplication.run(PollingAgentApplication.class, args);
    }

    private final Environment environment;

    @Autowired
    public PollingAgentApplication(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public JiraClient jiraClient() {
        String username = environment.getRequiredProperty("jira.username");
        String password = environment.getRequiredProperty("jira.password");
        return new JiraClient(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.ALWAYS)
                        .authenticator(
                            // TODO: Authenticator does not add Authorization header, so doing it explicitly in the client
                                new Authenticator() {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(username, password.toCharArray());
                                    }
                                })
                        .build(),
                username,
                password
        );
    }

    @Bean
    public CallbackClient callbackClient() {
        return new CallbackClient(
                HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()
        );
    }

    @Bean
    @Scope("prototype")
    public JiraStatusJob.Builder jiraStatusJobBuilder(PollQueue pollQueue, JobRepository jobRepository, JobFactory jobFactory) {
        return new JiraStatusJob.Builder(
                jiraClient(),
                pollQueue,
                jobFactory,
                jobRepository
        );
    }

    @Bean
    @Scope("prototype")
    public CallbackJob.Builder callbackJobBuilder(PollQueue pollQueue, JobRepository jobRepository) {
        return new CallbackJob.Builder(callbackClient(), pollQueue, jobRepository);
    }

    @Bean
    public File repositoryDirectory() {
        return environment.getRequiredProperty("repositoryDirectory", File.class);
    }

}