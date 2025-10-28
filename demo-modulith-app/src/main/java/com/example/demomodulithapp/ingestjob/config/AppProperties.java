package com.example.demomodulithapp.ingestjob.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Io io;
    private final Schedule schedule;
    private final String timezone;

    public AppProperties(@DefaultValue Io io, @DefaultValue Schedule schedule,
                         @DefaultValue("Asia/Singapore") String timezone) {
        this.io = io;
        this.schedule = schedule;
        this.timezone = timezone;
    }

    public Io getIo() {
        return io;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public String getTimezone() {
        return timezone;
    }

    public static class Io {
        private final String inputPattern;
        private final String aggregatesPattern;
        private final String rejectsPattern;

        public Io(@DefaultValue("/data/in/orders_{dataDate}.csv") String inputPattern,
                  @DefaultValue("/data/out/{dataDate}/aggregates.csv") String aggregatesPattern,
                  @DefaultValue("/data/out/{dataDate}/rejects.csv") String rejectsPattern) {
            this.inputPattern = inputPattern;
            this.aggregatesPattern = aggregatesPattern;
            this.rejectsPattern = rejectsPattern;
        }

        public String getInputPattern() {
            return inputPattern;
        }

        public String getAggregatesPattern() {
            return aggregatesPattern;
        }

        public String getRejectsPattern() {
            return rejectsPattern;
        }
    }

    public static class Schedule {
        private final String cron;

        public Schedule(@DefaultValue("0 0 20 * * *") String cron) {
            this.cron = cron;
        }

        public String getCron() {
            return cron;
        }
    }
}
