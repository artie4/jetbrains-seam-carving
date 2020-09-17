package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO


fun main(args: Array<String>) {

    val inputFile = args[1]
    val outputFile = args[3]

    val positiveImage = ImageIO.read(Files.newInputStream(Path.of(inputFile)))

    val width: Int = positiveImage.getWidth()
    val height: Int = positiveImage.getHeight()

    // Constructs a BufferedImage of one of the predefined image types.
    val negativeImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    for (row in 0 until width) {
        for (col in 0 until height) {
            val rgba: Int = positiveImage.getRGB(row, col)
            var color = Color(rgba, true)
            color = Color(255 - color.red,
                    255 - color.green,
                    255 - color.blue)
            negativeImage.setRGB(row, col, color.rgb)
        }
    }

    val file = File(outputFile)
    ImageIO.write(negativeImage, "bmp", file)

}