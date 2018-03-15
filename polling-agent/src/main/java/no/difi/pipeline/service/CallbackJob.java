package no.difi.pipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;

public class CallbackJob implements Job {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ZonedDateTime created = ZonedDateTime.now();
    private URL address;
    private String onBehalfOf;
    private CallbackClient callbackClient;
    private PollQueue pollQueue;
    private JobRepository jobRepository;

    private CallbackJob() {
        // Use builder
    }

    @SuppressWarnings("unused")
    public URL getAddress() {
        return address;
    }

    @SuppressWarnings("unused")
    public String getOnBehalfOf() {
        return onBehalfOf;
    }

    public void execute() {
        CallbackClient.Response response = callbackClient.callback(address);
        if (response.ok()) {
            logger.info("Callback to {} accepted [{} ms]", address, response.requestDuration());
            jobRepository.delete(this.id());
        } else if (response.notFound() && SECONDS.between(created, now()) < 10L) {
            logger.info(
                    "Callback listener {} not found -- assuming it is not set up yet ({} seconds since job was registered) [{} ms]",
                    address,
                    SECONDS.between(created, now()),
                    response.requestDuration()
            );
            newPollIn(2);
        } else if (response.notFound()) {
            logger.info("Callback listener {} is gone [{} ms]", address, response.requestDuration());
            jobRepository.delete(this.id());
        } else {
            logger.warn("Callback to {} failed: {} -- retrying in a minute", address, response.errorDetails());
            newPollIn(60);
        }
    }

    private void newPollIn(int seconds) {
        pollQueue.add(this, seconds);
    }

    @Override
    public String id() {
        return getClass().getSimpleName() + "-" + onBehalfOf;
    }

    public interface OnBehalfOf {
        CallbackAddress onBehalfOf(String job);
    }

    public interface CallbackAddress {
        CallbackJob to(URL callbackAddress);
    }

    public static class Builder implements OnBehalfOf, CallbackAddress {

        private CallbackJob instance = new CallbackJob();

        public Builder(CallbackClient callbackClient, PollQueue pollQueue, JobRepository jobRepository) {
            instance.callbackClient = callbackClient;
            instance.pollQueue = pollQueue;
            instance.jobRepository = jobRepository;
        }

        public CallbackAddress onBehalfOf(String job) {
            instance.onBehalfOf = job;
            return this;
        }

        @Override
        public CallbackJob to(URL callbackAddress) {
            instance.address = callbackAddress;
            return instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackJob that = (CallbackJob) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id());
    }

    @Override
    public String toString() {
        return id();
    }

}
