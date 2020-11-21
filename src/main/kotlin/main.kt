package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.sqrt

// -in sky.png -out sky-energy.png
fun main(args: Array<String>) {

    val inputFile = args[1]
    val outputFile = args[3]

    val originalImage = ImageIO.read(Files.newInputStream(Paths.get(inputFile)))

    val width: Int = originalImage.width
    val height: Int = originalImage.height

    val energyImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

    var maxEnergyValue = 0.0

    val energyArr = Array(width) { Array(height) { 0.0 } }

    if (width < 3 || height < 3) {
        throw RuntimeException("Too small image")
    }

    for (row in 0 until width) {
        for (col in 0 until height) {
            var xRGBRigth: Int
            var xRGBLeft: Int
            var yRGBTop: Int
            var yRGBBottom: Int

            if (row == 0) {
                xRGBLeft = originalImage.getRGB(row, col)
                xRGBRigth = originalImage.getRGB(row + 2, col)
            } else if (row == width - 1) {
                xRGBLeft = originalImage.getRGB(row - 2, col)
                xRGBRigth = originalImage.getRGB(row, col)
            } else {
                xRGBLeft = originalImage.getRGB(row - 1, col)
                xRGBRigth = originalImage.getRGB(row + 1, col)
            }
            if (col == 0) {
                yRGBTop = originalImage.getRGB(row, col)
                yRGBBottom = originalImage.getRGB(row, col + 2)
            } else if (col == height - 1) {
                yRGBTop = originalImage.getRGB(row, col - 2)
                yRGBBottom = originalImage.getRGB(row, col)
            } else {
                yRGBTop = originalImage.getRGB(row, col - 1)
                yRGBBottom = originalImage.getRGB(row, col + 1)
            }

            val xColorLeft = Color(xRGBLeft, true)
            val xColorRight = Color(xRGBRigth, true)
            val yColorTop = Color(yRGBTop, true)
            val yColorBottom = Color(yRGBBottom, true)

            val xRed2 = (xColorLeft.red - xColorRight.red).toDouble().pow(2.0)
            val xGreen2 = (xColorLeft.green - xColorRight.green).toDouble().pow(2.0)
            val xBlue2 = (xColorLeft.blue - xColorRight.blue).toDouble().pow(2.0)

            val yRed2 = (yColorBottom.red - yColorTop.red).toDouble().pow(2.0)
            val yGreen2 = (yColorBottom.green - yColorTop.green).toDouble().pow(2.0)
            val yBlue2 = (yColorBottom.blue - yColorTop.blue).toDouble().pow(2.0)

            val xR2 = xRed2 + xGreen2 + xBlue2
            val yR2 = yRed2 + yGreen2 + yBlue2

            val energy = sqrt(xR2 + yR2)

            energyArr[row][col] = energy
            if (maxEnergyValue < energy) {
                maxEnergyValue = energy
            }
        }
    }

    for (row in 0 until width) {
        for (col in 0 until height) {

            val energy = energyArr[row][col]

            val intensity = (255.0 * energy / maxEnergyValue).toInt()
            val newColor = Color(intensity, intensity, intensity)
            energyImage.setRGB(row, col, newColor.rgb)
        }
    }

    val file = File(outputFile)
    ImageIO.write(energyImage, "bmp", file)

}