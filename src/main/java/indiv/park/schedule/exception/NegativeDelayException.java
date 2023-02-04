package indiv.park.schedule.exception;

public class NegativeDelayException extends RuntimeException {

	private static final long serialVersionUID = -6385315889186028736L;

	public NegativeDelayException() {
		super("지연 값은 음수일 수 없습니다.");
	}
}
