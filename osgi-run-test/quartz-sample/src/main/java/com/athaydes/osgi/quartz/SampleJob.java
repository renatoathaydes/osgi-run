package com.athaydes.osgi.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Sample Quartz job.
 */
public class SampleJob implements Job
{

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException
    {
        System.out.println("Running job " +
                context.getJobDetail().getKey().getName());
    }


}
