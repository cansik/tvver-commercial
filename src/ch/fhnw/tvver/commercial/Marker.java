package ch.fhnw.tvver.commercial;

import java.awt.RenderingHints;
import java.io.File;
import java.util.List;

import ch.fhnw.ether.image.FloatFrame;
import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.image.ImageScaler;
import ch.fhnw.ether.image.RGB8Frame;
import ch.fhnw.ether.image.Frame.FileFormat;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.util.Log;
import ch.fhnw.util.math.MathUtilities;

public class Marker extends AbstractDetector {
	private static final Log log = Log.create();

	private Frame marker;

	public Marker() {
		try {
			marker = Frame.create(Marker.class.getResource("marker.png"));
		} catch(Throwable t) {
			log.severe(t);
		}
	}

	private final byte[]     rgb0 = new byte[3];
	private final byte[]     rgb1 = new byte[3];
	private       double     start;
	private final FloatFrame corner = new FloatFrame(80, 40);
	
	/**
	 * Process a video frame
	 * @param videoFrame The video frame to process.
	 * @param result Add detected segments to this list.
	 */
	@Override
	protected void process(VideoFrame videoFrame, List<Segment> result) {
		// get video frame pixels
		Frame frame = videoFrame.getFrame();
		// scale marker if necessary
		if(frame.width != marker.width || frame.height != marker.height)
			marker = ImageScaler.getScaledInstance(marker, frame.width, frame.height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
		// only look at center region
		int x = frame.width / 4;
		int y = frame.height / 4;
		int w = frame.width / 2;
		int h = frame.height / 2;
		long delta = 0;
		// calculate frame difference limit
		long limit = frame.width * frame.height * 10;
		// compute (frame-marker)^2
		for(int i = 0; i < w; i++) {
			for(int j = 0; j < h; j++) {
				frame.getRGB(x + i, y + j, rgb0);
				marker.getRGB(x + i, y + j, rgb1);
				int b0 = (rgb0[0] & 0xFF) + (rgb0[1] & 0xFF) + (rgb0[2] & 0xFF);
				int b1 = (rgb1[0] & 0xFF) + (rgb1[1] & 0xFF) + (rgb1[2] & 0xFF);
				delta += (b0 - b1) * (b0 - b1);
			}
			if(delta > limit) // fast fail
				break;
		}
		// accumulate corner image
		x = frame.width  - corner.width;
		y = frame.height - corner.height;
		for(int i = corner.width; --i >= 0;)
			for(int j = corner.height; --j >= 0;) {
				corner.setBrightness(i, j, corner.getBrightness(i, j) + frame.getBrightness(x + i, y + j));
			}
		// if marker is found, create segment
		if(delta < limit) {
			System.out.println("New segment at " + start  + " duration:" + (videoFrame.playOutTime - start));
			addSegment(videoFrame.playOutTime, result);
			start = videoFrame.playOutTime;
		}
		// create final segment
		if(videoFrame.isLast())
			addSegment(videoFrame.playOutTime, result);
		// classify segments
		for(Segment segment: result)
			segment.setCommercial(isCommercial(segment));
	}

	/**
	 * Add a segment to the result list
	 * 
	 * @param time The current frame time.
	 * @param result The result list where the segment will be added.
	 */
	private void addSegment(double time, List<Segment> result) {
		try {
			// normalize corner image
			corner.normalize();
			// write corner image for debugging
			corner.write(new File(getBaseName() + "_corner_" + (1000 + result.size())+ ".png"), FileFormat.PNG);
			// write histogram for debugging
			histogram(corner, 64).write(new File(getBaseName() + "_hist_" + (1000 + result.size())+ ".png"), FileFormat.PNG);
			// create segment
			Segment segment = new Segment(start, time - start);
			// save corner data for classification
			segment.setUserData(corner.copy());
			// add segment to result list
			result.add(segment);
			// clear corner for next segment
			corner.clear();
		} catch(Throwable t) {
			log.severe(t);
		}
	}

	/**
	 * Create a histogram of a float image.
	 * 
	 * @param frame
	 * @param size
	 * @return
	 */
	private Frame histogram(FloatFrame frame, int size) {
		final int size1 = size - 1;
		Frame result = new RGB8Frame(size, size);
		int[] bins = new int[size];
		for(int i = frame.width; --i >= 0;)
			for(int j = frame.height; --j >= 0;)
				bins[MathUtilities.clamp((int) (frame.getBrightness(i, j) * size1), 0, size1)]++;
		float max = 0;
		for(int h : bins)
			max = Math.max(max, h);
		for(int i = size; --i >= 0;)
			for(int j = size; --j >= 0;) {
				float lim = bins[i] / max;
				result.setARGB(i, j, j / (float)size > lim ? -1 : 0);
			}
		return result;
	}

	private boolean isCommercial(Segment segment) {
		return false;
	}
}
