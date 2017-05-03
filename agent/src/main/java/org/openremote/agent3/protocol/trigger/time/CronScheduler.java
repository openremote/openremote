/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent3.protocol.trigger.time;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

class CronScheduler {

    static class CronJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Runnable executeAction = (Runnable)context.getMergedJobDataMap().get("ACTION");

            if (executeAction != null) {
                LOG.info("Cron trigger fired: " + context.getTrigger().getKey().getName());
                executeAction.run();
            }
        }
    }


    private static final Logger LOG = Logger.getLogger(CronScheduler.class.getName());
    protected static final int ALARM_UPDATE_DELAY_MS = 10000;
    protected final List<String> jobIds = new ArrayList<>();
    protected final org.quartz.Scheduler scheduler;

    CronScheduler() {
        org.quartz.Scheduler tempScheduler;

        // Set properties for cronScheduler factory
        Properties schedulerProperties = new Properties();
        schedulerProperties.setProperty("org.quartz.threadPool.threadCount", "1");
        // Use default thread pool for now
        //schedulerProperties.setProperty("org.quartz.threadPool.class", null);

        try {
            // Initialise Quartz CronScheduler
            SchedulerFactory factory = new StdSchedulerFactory(schedulerProperties);
            tempScheduler = factory.getScheduler();
        } catch (SchedulerException e) {
            tempScheduler = null;
            LOG.log(Level.WARNING, "Failed to create quartz cronScheduler", e);
        }

        scheduler = tempScheduler;
    }

    protected boolean isValid() {
        return scheduler != null;
    }

    protected void shutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(false);
            } catch (SchedulerException e) {
                LOG.log(Level.FINE, "Exception has occurred during force shutdown of quartz cronScheduler", e);
            }
        }
    }

    protected void addOrReplaceJob(String id, CronExpression expression, Runnable executeHandler) {
        if (jobIds.contains(id)) {
            removeJob(id);
        }

        CronTrigger cronTrigger = createCronTrigger(id, expression, executeHandler);

        try {
            scheduler.scheduleJob(cronTrigger);

            if (!scheduler.isStarted()) {
                LOG.info("Starting the cron cronScheduler");
                scheduler.start();
            }

            jobIds.add(id);
        } catch (SchedulerException e) {
            LOG.log(Level.WARNING, "Unable to start cron job: " + id, e);
        }
    }

    protected void removeJob(String id) {
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(id));
        } catch (SchedulerException e) {
            LOG.log(Level.FINE, "Exception thrown whilst trying to unschedule cron job: " + id, e);
        } finally {
            jobIds.remove(id);
        }
    }

    protected static CronTrigger createCronTrigger(String id, CronExpression cronExpression, Runnable executeHandler) {
        if (cronExpression == null) {
            LOG.info("Cron expression is null so cannot create trigger: " + id);
            return null;
        }

        // Create the cron schedule
        CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(cronExpression);

        // Create the job data (just the execute handler
        JobDataMap jobData = new JobDataMap();
        jobData.put("ACTION", executeHandler);

        // Create the execution job
        JobDetail cronJob = JobBuilder.newJob(CronJob.class)
            .withIdentity("cronJob1", id)
            .setJobData(jobData)
            .build();

        // Create the cron trigger for this alarm
        return TriggerBuilder.newTrigger()
            .withIdentity(TriggerKey.triggerKey(id))
            .withSchedule(cronSchedule)
            .forJob(cronJob)
            .build();
    }
}