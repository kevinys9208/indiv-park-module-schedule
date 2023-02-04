package indiv.park.schedule;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.reflections.Reflections;

import indiv.park.schedule.annotation.ScheduleJob;
import indiv.park.schedule.exception.NegativeDelayException;
import indiv.park.schedule.exception.SameJobNameException;
import indiv.park.starter.annotation.Module;
import indiv.park.starter.inheritance.ModuleBase;
import lombok.extern.slf4j.Slf4j;

@Module(name = "schedule")
@Slf4j
public final class ScheduleModule implements ModuleBase {

	public static final ScheduleModule INSTANCE = new ScheduleModule();
	
	private ScheduleModule() {}
	
	private final Map<String, ScheduleSet> scheduleMap = new ConcurrentHashMap<>();
	private Object configuration;
	private Scheduler scheduler;
	
	private final String not_exist = "{} 스케줄이 존재하지 않습니다.";
	private final String job_execute = "{} 스케줄이 실행되었습니다.";
	
	@Override
	public void initialize(Class<?> mainClass) throws SchedulerException  {
		Reflections reflections = new Reflections(mainClass.getPackage().getName());
		
		loadScheduler();
		loadJobList(reflections);
	}

	@Override
	public void setConfiguration(Object configuration) {
		this.configuration = configuration;
	}
	
	private void loadScheduler() throws SchedulerException {
		if (configuration == null) {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			
		} else {
			Properties schedulerProperties = new Properties();
			schedulerProperties.putAll((Map<?, ?>) configuration);
			
			scheduler = new StdSchedulerFactory(schedulerProperties).getScheduler();
		}
	}
	
	private void loadJobList(Reflections reflections) throws SchedulerException {
		Set<Class<?>> scheduleSet = reflections.getTypesAnnotatedWith(ScheduleJob.class);
		if (scheduleSet.size() == 0) {
			logger.info("구성 가능한 스케줄이 존재하지 않습니다.");
			return;
		}
		
		showFoundSchedule(scheduleSet);
		
		for (Class<?> clazz : scheduleSet) {
			ScheduleJob scheduleJob = clazz.getAnnotation(ScheduleJob.class);
			
			String name = scheduleJob.name();
			String group = scheduleJob.group();
			String cron = scheduleJob.cron().trim();
			
			addSchedule(clazz, name, group, cron);
		}
	}
	
	private void showFoundSchedule(Set<Class<?>> scheduleSet) {
		final String found = "확인된 스케줄 : {}";
		
		for (Class<?> clazz : scheduleSet) {
			logger.info(found, clazz.getSimpleName());
		}
	}
	
	public void start() {
		start(0);
	}
	
	public void start(int delay) {
		try {
			if (delay < 0) {
				throw new NegativeDelayException();
			}
			if (delay == 0) {
				scheduler.start();
				return;
			}
			scheduler.startDelayed(delay);
			
		} catch (Exception e) {
			logger.error("스케줄러를 시작하던 중 예외가 발생하였습니다. [ {} ]", e.toString());
		}
	}
	
	public void shutdownGracefully() {
		try {
			scheduler
				.getJobKeys(GroupMatcher.anyGroup())
				.forEach(jobKey -> {
					try {
						scheduler.interrupt(jobKey);
						
					} catch (UnableToInterruptJobException e) {
						e.printStackTrace();
					}
				});
			
			scheduler.shutdown(true);
			
			logger.info("스케줄러를 안전하게 종료하였습니다.");
			
		} catch (Exception e) {
			logger.error("스케줄러를 종료하던 중 예외가 발생하였습니다. [ {} ]", e.toString());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addSchedule(Class<?> job, String jobName, String groupName, String cron) throws SchedulerException {
		JobDetail jobDetail = JobBuilder.newJob((Class<? extends Job>) job)
										.withIdentity(jobName, groupName)
										.build();

		Trigger trigger = TriggerBuilder.newTrigger()
										.withIdentity(jobName + "Trigger", groupName)
										.withSchedule(CronScheduleBuilder.cronSchedule(cron).withMisfireHandlingInstructionIgnoreMisfires())
										.build();
		
		scheduler.scheduleJob(jobDetail, trigger);
		ScheduleSet scheduleSet = scheduleMap.putIfAbsent(jobName, new ScheduleSet(jobDetail, trigger));
		if (scheduleSet != null) {
			throw new SameJobNameException(jobName);
		}
	}
	
	public void removeSchedule(String jobName) {
		try {
			ScheduleSet scheduleSet = scheduleMap.get(jobName);
			if (scheduleSet == null) {
				logger.error(not_exist, jobName);
				return;
			}
			
			JobKey jobKey = scheduleSet.getJobDetail().getKey();
			scheduler.deleteJob(jobKey);
			scheduleMap.remove(jobName);
			logger.info("{} 스케줄이 제거되었습니다.", jobName);
			
		} catch (Exception e) {
			logger.error("{} 스케줄을 제거하던 중 예외가 발생하였습니다. [ {} ]", jobName, e.toString());
		}
	}
	
	public void pauseSchedule(String jobName) {
		try {
			ScheduleSet scheduleSet = scheduleMap.get(jobName);
			if (scheduleSet == null) {
				logger.error(not_exist, jobName);
				return;
			}
			
			scheduler.pauseJob(scheduleSet.getJobDetail().getKey());
			logger.info("{} 스케줄이 일시중지되었습니다.", jobName);
			
		} catch (Exception e) {
			logger.error("{} 스케줄을 일시중지하던 중 예외가 발생하였습니다. [ {} ]", jobName, e.toString());
		}
	}
	
	public void resumeSchedule(String jobName) {
		try {
			ScheduleSet scheduleSet = scheduleMap.get(jobName);
			if (scheduleSet == null) {
				logger.error(not_exist, jobName);
				return;
			}
			
			scheduler.resumeJob(scheduleSet.getJobDetail().getKey());
			logger.info("{} 스케줄이 재시작되었습니다.", jobName);
			
		} catch (Exception e) {
			logger.error("{} 스케줄을 재시작하던 중 예외가 발생하였습니다. [ {} ]", jobName, e.toString());
		}
	}
	
	public void rescheduleJob(String jobName, Trigger newTrigger) {
		try {
			ScheduleSet scheduleSet = scheduleMap.get(jobName);
			if (scheduleSet == null) {
				logger.error(not_exist, jobName);
				return;
			}
			
			scheduler.rescheduleJob(scheduleSet.getTrigger().getKey(), newTrigger);
			scheduleSet.setTrigger(newTrigger);
			logger.info("{} 스케줄이 재설정되었습니다.", jobName);
			
		} catch (Exception e) {
			logger.error("{} 스케줄을 재설정하던 중 예외가 발생하였습니다. [ {} ]", jobName, e.toString());
		}
	}
	
	public void executeJob(String jobName) {
		try {
			ScheduleSet scheduleSet = scheduleMap.get(jobName);
			if (scheduleSet == null) {
				logger.error(not_exist, jobName);
				return;
			}
			
			scheduler.triggerJob(scheduleSet.getJobDetail().getKey());
			logger.info(job_execute, jobName);
			
		} catch (Exception e) {
			logger.error("{} 스케줄을 실행하던 중 예외가 발생하였습니다. [ {} ]", jobName, e.toString());
		}
	}
	
	public void executeGroupJob(String groupName) {
		try {
			Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
			for (JobKey jobKey : jobKeys) {
				scheduler.triggerJob(jobKey);
				logger.info(job_execute, jobKey.getName());
			}
			
		} catch (Exception e) {
			logger.error("{} 그룹 스케줄을 실행하던 중 예외가 발생하였습니다. [ {} ]", groupName, e.toString());
		}
	}
	
	public void executeAllJob() {
		try {
			Set<JobKey> jobKeys = scheduler .getJobKeys(GroupMatcher.anyGroup());
			for (JobKey jobKey : jobKeys) {
				scheduler.triggerJob(jobKey);
				logger.info(job_execute, jobKey.getName());
			}
			
		} catch (Exception e) {
			logger.error("모든 스케줄을 실행하던 중 예외가 발생하였습니다. [ {} ]", e.toString());
		}
	}
}