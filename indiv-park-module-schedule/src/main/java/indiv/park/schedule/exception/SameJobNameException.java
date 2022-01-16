package indiv.park.schedule.exception;

public class SameJobNameException extends RuntimeException {

	private static final long serialVersionUID = 8246953327830700828L;

	public SameJobNameException() {
		super("동일한 이름의 스케줄이 존재합니다.");
	}
}
