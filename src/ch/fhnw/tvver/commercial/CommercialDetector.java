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

public class CommercialDetector extends AbstractDetector
{
	final int ELAPSED_TIME = 1000;
	final int TRAINING_TIME = 400;

	double start;
	double duration;

	BufferedImage diffImage = null;

	boolean commercial = false;
	boolean training;

	int counter = 0;
	int elapsedFrames = 0;
	int commercialTime = 0;
	
	@Override
	protected void init(URLVideoSource videoSource, File storageDirectory, boolean training) {
		training = true;
		this.training = training;
	}
	
	@Override
	protected void process(VideoFrame videoFrame, float[] audio, List<Segment> result) {
		// process video frame

		if (videoFrame.isKeyframe()) {
			out(counter + ": Key!");
		}

		if(training)
		{
			train(videoFrame);
		}
		else
		{
			BufferedImage binaryFrame = getFramePart(videoFrame.getFrame());
			double sameness = compareImages(diffImage, binaryFrame);

			if(commercial)
			{
				commercialTime++;
			}

			//todo: set threshold
			if(sameness < 0.9) {
				if (!commercial) {
					elapsedFrames = 0;
					commercial = true;
					System.out.println("Werbung ("+sameness + "): " + counter);
					writeFrame(videoFrame, "comm_start");
					writeImage(binaryFrame, "comm_start_binary");
				}
			}
			else
			{
				if(commercial && elapsedFrames > ELAPSED_TIME)
				{
					commercial = false;
					System.out.println("Werbung ende ("+ sameness + "): " + counter);
					writeFrame(videoFrame, "comm_end");
					writeImage(binaryFrame, "comm_end_binary");
				}
			}
		}

		// create and add segment when commercial/non-commercial block is detected
		Segment segment = new Segment(start, duration);
		result.add(segment);
		if (videoFrame.isLast()) {
			// do some post-processing on segments here
			out("Commercial Time: " + commercialTime);
		}

		if(counter == TRAINING_TIME)
		{
			out("training finished!");
			training = false;
			writeImage(diffImage, "trained");
		}

		elapsedFrames++;
		counter++;
	}

	/***
	 * Returns percentage value of sameness.
	 * @param img1 Must be Diff Image (Main)
	 * @param img2
     * @return
     */
	double compareImages(BufferedImage img1, BufferedImage img2)
	{
		int sameCount = 0;
		int blackCount = 0;

		for (int v = 0; v < img1.getHeight(); v++) {
			for (int u = 0; u < img1.getWidth(); u++) {
				int img1Value = img1.getRGB(u, v);
				int img2Value = img2.getRGB(u, v);

				if(img1Value == Color.BLACK.getRGB()) {
					blackCount++;
					if (img1Value == img2Value) {
						sameCount++;
					}
				}
			}
		}

		return (double)sameCount / blackCount;
	}

	BufferedImage getFramePart(Frame frame)
	{
		//todo: calculate dynamically
		int offsetX = frame.width; //200;
		int offsetY = frame.height; //200;

		BufferedImage image = new BufferedImage(offsetX, offsetY, BufferedImage.TYPE_INT_RGB);

		//get upper right corner
		int x = frame.width - offsetX;
		int y = frame.height - offsetY;

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

		return image;
	}

	void train(VideoFrame videoFrame)
	{
		Frame frame = videoFrame.getFrame();
		BufferedImage image = getFramePart(frame);

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

		if(counter % 10 == 0) {
			writeImage(diffImage, "diff");
		}
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

	void writeFrame(VideoFrame videoFrame, String name)
	{
		try {
			videoFrame.getFrame().write(new File("frames/" + counter + "_" + name +".png"), Frame.FileFormat.PNG);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void writeImage(BufferedImage image, String name)
	{
		try {
			ImageIO.write(image, "PNG", new File("frames/" + counter + "_" + name + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static BufferedImage copyImage(BufferedImage source){
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		Graphics g = b.getGraphics();
		g.drawImage(source, 0, 0, null);
		g.dispose();
		return b;
	}
}
