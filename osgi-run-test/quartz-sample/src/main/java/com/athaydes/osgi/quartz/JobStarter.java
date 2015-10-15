package com.athaydes.osgi.quartz;

import org.quartz.*;
import org.osgi.framework.*;
import org.quartz.impl.StdSchedulerFactory;

public class JobStarter implements BundleActivator
{

    Scheduler _scheduler;

    @Override
    public void start(BundleContext context) throws Exception
    {
        System.out.println("STARTING JOB_STARTER");
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("dummyTriggerName", "group1")
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(5).repeatForever())
                .build();

        _scheduler = new StdSchedulerFactory().getScheduler();
        _scheduler.start();

        JobDetail job = JobBuilder.newJob(SampleJob.class)
                .withIdentity("dummyJobName", "group1").build();

        _scheduler.scheduleJob(job, trigger);

        System.out.println("Scheduled the job!!!");
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        System.out.println("Stopping quartz-demo bundle");
        _scheduler.shutdown();
    }

}
