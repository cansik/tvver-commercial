/*
 * Copyright (c) 2013 - 2015 Stefan Muller Arisona, Simon Schubiger, Samuel von Stachelski
 * Copyright (c) 2013 - 2015 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.fhnw.tvver.commercial;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.image.Frame.FileFormat;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.ITimebase;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.PreviewTarget;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.tvver.commercial.AbstractDetector.Segment;
import ch.fhnw.util.TextUtilities;

/**
 * Test class for tvver commercial detection project.
 * 
 * @author sschubiger
 *
 */
public class Main {
	private static final boolean CREATE_PREVIEWS = true;

	public static void main(String[] args) throws Throwable {
		if(args.length <= 2) {
			System.out.println("Usage:" + Main.class.getName() + " <detector class> <video file>");
			System.exit(1);
		}

		// Create detector
		AbstractDetector detector = (AbstractDetector) Class.forName(Main.class.getPackage().getName() + "." + args[0]).newInstance();
		// Detect commercials
		List<Segment> result = new ArrayList<>();
		long time = detector.detect(new URLVideoSource(new File(args[1]).toURI().toURL(), 1), result);
		System.out.println("Time: " + time / ITimebase.SEC2NS + " sec");

		// create resulting edl file
		try(FileWriter out = new FileWriter(new File(TextUtilities.stripFileExtension(args[1]) + "_segments.edl"))) {
			for(Segment s : result)
				if(s.commercial)
					out.write(s.start + "\t" + (s.start + s.duration) + "\t" + EDLFile.COMMERCIAL_BREAK + "\n");
		}

		// create previews for debugging
		if(CREATE_PREVIEWS) {
			IVideoSource src = new URLVideoSource(new File(args[1]).toURI().toURL());
			RenderProgram<IVideoRenderTarget> program = new RenderProgram<>(src);
			int i = 0;
			for(Segment s : result) {
				program.setTarget(null);
				final PreviewTarget target  = new PreviewTarget(4096, 64, s.duration);
				File file = new File(TextUtilities.stripFileExtension(args[1]) + "_" + (i + 1000) + (s.commercial ? "c.png" : ".png"));
				System.out.println("Writing '" + file.getName() + "' " + i + "/" + result.size() + " (" + s + ")...");
				target.useProgram((RenderProgram<IVideoRenderTarget>)program);
				target.start();
				target.sleepUntil(IScheduler.NOT_RENDERING);
				target.getPreview().write(file, FileFormat.PNG);
				i++;
			}
		}
		System.out.println("done");
		System.exit(0);
	}
}
