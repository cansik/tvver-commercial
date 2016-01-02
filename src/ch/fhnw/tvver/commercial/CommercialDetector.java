package ch.fhnw.tvver.commercial;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.VideoFrame;

import javax.imageio.ImageIO;

public class CommercialDetector extends AbstractDetector {
	double start;
	double duration;

	int counter = 0;

	BufferedImage diffImage = null;
	
	@Override
	protected void init(URLVideoSource videoSource, File storageDirectory, boolean training) {
	}
	
	@Override
	protected void process(VideoFrame videoFrame, float[] audio, List<Segment> result) {
		// process video frame
		Frame frame = videoFrame.getFrame();

		if (videoFrame.isKeyframe()) {
			out(counter + ": Key!");
		}

		//todo: calculate dynamically
		int offsetX = 71;
		int offsetY = 96;

		BufferedImage image = new BufferedImage(offsetX, offsetY, BufferedImage.TYPE_INT_RGB);

		//get upper right corner
		int x = frame.width - offsetX;
		int y = frame.height - offsetY;
		int w = frame.width;
		int h = frame.height;

		for (int i = 0; i < offsetX; i++) {
			for (int j = 0; j < offsetY; j++) {

				byte[] currentPixel = new byte[3];

				//get pixel from current frame
				frame.getRGB(x + i, y + j, currentPixel);

				// create grayscale image
				int color = binarize(grayscale(currentPixel));
				int rgb = color | (color << 8) | (color << 16);

				//draw
				image.setRGB(i, offsetY - 1 - j, rgb);
			}
		}

		if (diffImage == null) {
			out("set diff image!");
			diffImage = copyImage(image);
		} else {
			//compare both images
			for (int v = 0; v < image.getHeight(); v++) {
				for (int u = 0; u < image.getWidth(); u++) {
					int currentValue = image.getRGB(u, v);
					int storedValue = diffImage.getRGB(u, v);

					//todo: set good threshold
					if (currentValue == storedValue) {
						//set pixel the same color
						diffImage.setRGB(u, v, storedValue);
					} else {
						//set pixel white
						diffImage.setRGB(u, v, -1);
					}
				}
			}
		}

		try {
			ImageIO.write(image, "BMP", new File("frames/" + counter + "_aframe.bmp"));
			ImageIO.write(diffImage, "BMP", new File("frames/" + counter + "_bdiff.bmp"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create and add segment when commercial/non-commercial block is detected
		Segment segment = new Segment(start, duration);
		result.add(segment);
		if (videoFrame.isLast()) {
			// do some post-processing on segments here
		}

		counter++;
	}

	void out(Object obj)
	{
		System.out.println(obj);
	}

	int grayscale(byte[] pixel){
		double res = pixel[0]*0.299 +  pixel[1]*0.587 + pixel[2]*0.114;
		return (int)res;
	}

	int binarize(int b)
	{
		if(b > 0)
			return Color.WHITE.getRGB();
		else
			return Color.BLACK.getRGB();
	}

	public static BufferedImage copyImage(BufferedImage source){
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		Graphics g = b.getGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return b;
	}
}
