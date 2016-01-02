package ch.fhnw.tvver.commercial;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.audio.NullAudioTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
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
import ch.fhnw.util.FloatList;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public abstract class AbstractDetector extends AbstractVideoTarget {
	private static final Log log = Log.create();
	
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

	class AudioCapture extends AbstractRenderCommand<IAudioRenderTarget> {
		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
			audio.get().addAll(target.getFrame().samples);
		}
	}
	
	private List<Segment>   result;
	private String          baseName;
	private String          basePath;
	private long            time;
	private AtomicReference<FloatList> audio = new AtomicReference<FloatList>(new FloatList());
	
	protected AbstractDetector() {
		super(Thread.MIN_PRIORITY, AbstractVideoFX.FRAMEFX, false);
	}

	@Override
	public void render() throws RenderCommandException {
		final VideoFrame frame = getFrame();
		long before = System.nanoTime();
		FloatList tmp = audio.getAndSet(new FloatList());
		process(frame, tmp.toArray(), result);
		time += System.nanoTime() - before;
		if(frame.isLast())
			stop();
	}
	
	public final void initInternal(URLVideoSource videoSource, File storageDirectory, boolean training) {
		File movie;
		try {
			movie = new File(videoSource.getURL().toURI());
			basePath = TextUtilities.stripFileExtension(movie.getAbsolutePath());
			baseName = TextUtilities.getFileNameWithoutExtension(basePath);
			init(videoSource, storageDirectory, training);
		} catch (Throwable t) {
			log.severe(t);
		}
	}
	
	protected abstract void init(URLVideoSource videoSource, File storageDirectory, boolean training);
	
	protected abstract void process(VideoFrame videoFrame, float[] audio, List<Segment> result);

	public long detect(URLVideoSource videoSource, List<Segment> result) throws Throwable {
		this.time       = 0;
		this.result     = result;
		
		RenderProgram<IAudioRenderTarget> audioProgram = new RenderProgram<>((IAudioSource)videoSource, new AudioCapture());
		RenderProgram<IVideoRenderTarget> videoProgram = new RenderProgram<>((IVideoSource)videoSource);
		NullAudioTarget                   audioTarget  = new NullAudioTarget(videoSource.getNumChannels(), videoSource.getSampleRate());
		
		audioTarget.useProgram(audioProgram);
		audioTarget.setTimebase(this);
		audioTarget.start();
		
		new ParameterWindow(videoProgram, Flag.EXIT_ON_CLOSE);
		useProgram(videoProgram);
		start();
		sleepUntil(NOT_RENDERING);
		
		return time;
	}	

	protected String getBaseName() {
		return baseName;
	}
	
	public String getBasePath() {
		return basePath;
	}
}
