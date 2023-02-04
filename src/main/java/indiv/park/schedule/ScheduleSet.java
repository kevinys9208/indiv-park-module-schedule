package indiv.park.schedule;

import org.quartz.JobDetail;
import org.quartz.Trigger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ScheduleSet {

	private JobDetail jobDetail;
	private Trigger trigger;
}
