package ch.fhnw.tvver.commercial;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import ch.fhnw.ether.image.Frame.FileFormat;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.PreviewTarget;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.util.TextUtilities;

public class Preview {

	public static void main(String[] args) throws MalformedURLException, IOException, RenderCommandException {
		IVideoSource src = new URLVideoSource(new File(args[0]).toURI().toURL(), 1);
		RenderProgram<IVideoRenderTarget> program = new RenderProgram<>(src);
		final PreviewTarget target  = new PreviewTarget(4096, 64);
		File file = new File(TextUtilities.stripFileExtension(args[0]) + "_preview.png");
		target.useProgram((RenderProgram<IVideoRenderTarget>)program);
		target.start();
		target.sleepUntil(IScheduler.NOT_RENDERING);
		target.getPreview().write(file, FileFormat.PNG);
	}
}
