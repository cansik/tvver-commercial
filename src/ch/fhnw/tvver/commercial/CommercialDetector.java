package ch.fhnw.tvver.commercial;

import java.io.File;
import java.util.List;

import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.VideoFrame;

public class CommercialDetector extends AbstractDetector {
	double start;
	double duration;
	
	@Override
	protected void init(URLVideoSource videoSource, File storageDirectory, boolean training) {
	}
	
	@Override
	protected void process(VideoFrame videoFrame, float[] audio, List<Segment> result) {
		// process video frame
		// create and add segment when commercial/non-commercial block is detected
		Segment segment = new Segment(start, duration);
		result.add(segment);
		if(videoFrame.isLast()) {
			// do some post-processing on segments here
		}
	}
}
