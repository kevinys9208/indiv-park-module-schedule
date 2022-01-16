package indiv.park.schedule.inheritance;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

public abstract class JobBase implements InterruptableJob {
	
	private Thread thread;
	
	public abstract void operate(JobExecutionContext context) throws Exception;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			thread = Thread.currentThread();
			operate(context);
			
		} catch (Exception e) {
			throw new JobExecutionException(e.getMessage());
		}
	}
	
	@Override
	public void interrupt() throws UnableToInterruptJobException {
		if (thread != null) {
			thread.interrupt();
		}
	}
}