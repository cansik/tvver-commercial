package ch.fhnw.tvver.commercial;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.ether.ui.ParameterWindow.Flag;
import ch.fhnw.ether.video.AbstractVideoTarget;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.util.TextUtilities;

public abstract class AbstractDetector extends AbstractVideoTarget {
	private static final NumberFormat format = TextUtilities.decimalFormat(2); 

	public class Segment {
		public final double  start;
		public final double  duration;
		public       boolean commercial;
		private      Object  userData;

		public Segment(double start, double duration) {
			this(start, duration, false);
		}

		public Segment(double start, double duration, boolean commercial) {
			this.start     = start;
			this.duration   = duration;
			this.commercial = commercial;
		}

		@Override
		public String toString() {
			return "s:" + format.format(start) + " d:" + format.format(duration) + (commercial ? " COMMERCIAL" : "");
		}

		public void setCommercial(boolean commercial) {
			this.commercial = commercial;
		}

		public void setUserData(Object userData) {
			this.userData = userData;
		}

		public Object getUserData() {
			return userData;
		}
	}

	private List<Segment> result;
	private String        baseName;
	private long          time;

	protected AbstractDetector() {
		super(Thread.MIN_PRIORITY, AbstractVideoFX.FRAMEFX, false);
	}

	@Override
	public void render() throws RenderCommandException {
		final VideoFrame frame = getFrame();
		long before = System.nanoTime();
		process(frame, result);
		time += System.nanoTime() - before;
		if(frame.isLast())
			stop();
	}

	protected abstract void process(VideoFrame videoFrame, List<Segment> result);

	public long detect(URLVideoSource videoSource, List<Segment> result) throws Throwable {
		File movie = new File(videoSource.getURL().toURI());
		this.baseName   = TextUtilities.stripFileExtension(movie.getAbsolutePath());
		this.time       = 0;
		this.result     = result;
		
		RenderProgram<IVideoRenderTarget> program = new RenderProgram<>((IVideoSource)videoSource);
		new ParameterWindow(program, Flag.EXIT_ON_CLOSE);
		useProgram(program);
		start();
		sleepUntil(NOT_RENDERING);

		return time;
	}	

	protected String getBaseName() {
		return baseName;
	}
}
