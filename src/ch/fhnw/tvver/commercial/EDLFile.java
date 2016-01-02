package ch.fhnw.tvver.commercial;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Scanner;

import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.VideoFrame;

public class EDLFile extends AbstractDetector {
	public static final int CUT              = 0; // Cut
	public static final int MUTE             = 1; // Mute
	public static final int SCENE_MARK       = 2; // Scene Marker (if start and end times are specified, the end time is used)
	public static final int COMMERCIAL_BREAK = 3; // Commercial Break
	
	@Override
	public long detect(URLVideoSource videoSource, List<Segment> result) throws Throwable {
		long before = System.nanoTime();
		File edl   = new File(getBaseName() + ".edl");
		Scanner in = new Scanner(new FileReader(edl));
		double time  = 0;
		double start = 0;
		double end   = 0;
		while(in.hasNext()) {
			start      = in.nextDouble();
			end        = in.nextDouble();
			int action = in.nextInt();
			in.nextLine();
			
			if(action == CUT || action == COMMERCIAL_BREAK) {
				if(start > time)
					result.add(new Segment(time, start - time, false));
				result.add(new Segment(start, end-start, true));
				time = end;
			}
		}
		double duration = videoSource.getLengthInSeconds(); 
		if(duration > end)
			result.add(new Segment(end, duration - end, false));
		in.close();
		return System.nanoTime() - before;
	}

	@Override
	protected void process(VideoFrame frame, List<Segment> result) {}
}
