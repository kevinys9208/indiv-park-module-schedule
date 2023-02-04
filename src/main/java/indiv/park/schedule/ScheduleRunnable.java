package indiv.park.schedule;

import org.quartz.JobKey;
import org.quartz.Scheduler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduleRunnable implements Runnable {

	private final Scheduler scheduler;
	private final JobKey jobKey;
	
	public ScheduleRunnable(Scheduler scheduler, JobKey jobKey) {
		this.scheduler = scheduler;
		this.jobKey = jobKey;
	}
	
	@Override
	public void run() {
		try {
			scheduler.triggerJob(jobKey);
			logger.info("{} 스케줄이 트리거되었습니다.", jobKey.getName());
			
		} catch (Exception e) {
			logger.error("스케줄을 트리거하던 중 예외가 발생하였습니다. [ {} ]", e.toString());
		}
	}
}
