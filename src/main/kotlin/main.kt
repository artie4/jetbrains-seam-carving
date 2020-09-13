package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


fun main() {

    val scanner = Scanner(System.`in`)
    println("Enter rectangle width:")
    val width = scanner.nextLine().toInt()
    println("Enter rectangle height:")
    val height = scanner.nextLine().toInt()
    println("Enter output image name:")
    val outputFileName = scanner.nextLine()

    // Constructs a BufferedImage of one of the predefined image types.
    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    // Create a graphics which can be used to draw into the buffered image
    val g2d = bufferedImage.createGraphics()

    // add black background
    g2d.color = Color.black
    g2d.fillRect(0, 0, width, height)

    // paint main and second diagonals
    g2d.color = Color.RED

    if (width >= height) {
        val k = width / height
        for (i in 0..width step k) {
            g2d.fillRect(i, i / k, k, 1)
            g2d.fillRect(i, height - 1 - i / k, k, 1)
        }
    } else {
        val k = height / width
        for (i in 0..height step k) {
            g2d.fillRect(i / k, i, 1, k)
            g2d.fillRect(i / k, height - 1 - i / k, 1, k)
        }
    }

    // Disposes of this graphics context and releases any system resources that it is using.
    g2d.dispose()

    // Save as PNG
    val file = File(outputFileName)
    ImageIO.write(bufferedImage, "png", file)

}