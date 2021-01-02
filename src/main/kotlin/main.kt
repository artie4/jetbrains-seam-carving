package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.sqrt

fun main(args: Array<String>) {

    if (args.size < 8) {
        throw IllegalArgumentException("Wrong number of arguments. Required as next: -in sky.png -out sky-seam.png -width 20 -height 10")
    }

    val val1 = args[0]
    val val2 = args[1]
    val val3 = args[2]
    val val4 = args[3]
    val val5 = args[4]
    val val6 = args[5]
    val val7 = args[6]
    val val8 = args[7]
    val inputFile: String
    val outputFile: String
    val vSeamNumber: Int
    val hSeamNumber: Int
    when {
        val1 == "-in" && val3 == "-out" && val5 == "-width" && val7 == "-height" -> {
            inputFile = val2
            outputFile = val4
            vSeamNumber = val6.toInt()
            hSeamNumber = val8.toInt()
        }
        val3 == "-in" && val1 == "-out" && val5 == "-height" && val7 == "-width"  -> {
            inputFile = val4
            outputFile = val2
            vSeamNumber = val8.toInt()
            hSeamNumber = val6.toInt()
        }
        else -> {
            throw IllegalArgumentException("Wrong argument order. Required as next: -in sky.png -out sky-seam.png -width 20 -height 10")
        }
    }

    val originalImage = ImageIO.read(Files.newInputStream(Paths.get(inputFile)))

    var resizingImage = originalImage

    for (i in 0 until vSeamNumber) {
        val (maxPixelEnergy, pixelsEnergy) = computeEnergy(resizingImage)
        val minEnergyPath = minEnergyPath(pixelsEnergy)
        resizingImage = compressVertical(minEnergyPath, resizingImage)
    }

    for (i in 0 until hSeamNumber) {
        val (maxPixelEnergy, pixelsEnergy) = computeEnergy(resizingImage)
        val minEnergyPath = minEnergyHorizontalPath(pixelsEnergy)
        resizingImage = compressHorizontal(minEnergyPath, resizingImage)
    }

    File(outputFile).let { file -> ImageIO.write(resizingImage, file.extension, file) }


}

data class Pixel(val prev: Pixel?, val x: Int, val y: Int, val energyPath: Double)

fun dx2(x: Int, y: Int, image: BufferedImage): Int {
    return when (x) {
        0 -> {
            dx2(x + 1, y, image)
        }
        image.width - 1 -> {
            dx2(x - 1, y, image)
        }
        else -> {
            val left = Color(image.getRGB(x - 1, y))
            val right = Color(image.getRGB(x + 1, y))
            val redDiff = left.red - right.red
            val greenDiff = left.green - right.green
            val blueDiff = left.blue - right.blue
            redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff
        }
    }
}

fun dy2(x: Int, y: Int, image: BufferedImage): Int {
    return when (y) {
        0 -> {
            dy2(x, y + 1, image)
        }
        image.height - 1 -> {
            dy2(x, y - 1, image)
        }
        else -> {
            val top = Color(image.getRGB(x, y - 1))
            val bottom = Color(image.getRGB(x, y + 1))
            val redDiff = top.red - bottom.red
            val greenDiff = top.green - bottom.green
            val blueDiff = top.blue - bottom.blue
            redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff
        }
    }
}

fun pixelEnergy(x: Int, y: Int, image: BufferedImage): Double {
    return sqrt(dx2(x, y, image).toDouble() + dy2(x, y, image).toDouble())
}

fun computeEnergy(image: BufferedImage): Pair<Double, Array<DoubleArray>> {

    val width = image.width
    val height = image.height
    var maxPixelEnergy = 0.0

    val pixelsEnergy = Array(width) { DoubleArray(height) { 0.0 } }

    if (width < 3 || height < 3) {
        throw RuntimeException("Too small image")
    }

    for (row in 0 until width) {
        for (col in 0 until height) {
            val energy = pixelEnergy(row, col, image)
            pixelsEnergy[row][col] = energy
            if (maxPixelEnergy < energy) {
                maxPixelEnergy = energy
            }
        }
    }
    return maxPixelEnergy to pixelsEnergy
}

fun normalize(energyMatrix: Array<DoubleArray>, maxEnergyValue: Double): Array<DoubleArray> {
    energyMatrix.forEach {
        it.forEachIndexed { y, energy -> it[y] = 255.0 * energy / maxEnergyValue }
    }
    return energyMatrix
}

fun minEnergyPath(doubleMatrix: Array<DoubleArray>): Pixel {

    val copy = doubleMatrix.map { it.clone() }.toTypedArray()
    val (width, height) = copy.size to copy.first().size

    var matrixRow = Array(width) { Pixel(null, it, 0, doubleMatrix[it][0]) }

    for (y in 1 until height) {
        matrixRow = Array(width) { x ->
            val leftBorder = if (x == 0) x else x - 1
            val rightBorder = if (x == width - 1) x else x + 1
            val minEngPixel = listOf(matrixRow[leftBorder], matrixRow[x], matrixRow[rightBorder]).minByOrNull { it.energyPath }!!
            Pixel(minEngPixel, x, y, copy[x][y] + minEngPixel.energyPath)
        }
    }
    return matrixRow.minByOrNull { it.energyPath }!!
}

fun minEnergyHorizontalPath(doubleMatrix: Array<DoubleArray>): Pixel {

    val copy = doubleMatrix.map { it.clone() }.toTypedArray()
    val (width, height) = copy.size to copy.first().size

    var matrixColumn = Array(height) { Pixel(null, 0, it, doubleMatrix[0][it]) }

    for (x in 1 until width) {
        matrixColumn = Array(height) { y ->
            val upBorder = if (y == 0) y else y - 1
            val bottomBorder = if (y == height - 1) y else y + 1
            val minEngPixel = listOf(matrixColumn[upBorder], matrixColumn[y], matrixColumn[bottomBorder]).minByOrNull { it.energyPath }!!
            Pixel(minEngPixel, x, y, copy[x][y] + minEngPixel.energyPath)
        }
    }
    return matrixColumn.minByOrNull { it.energyPath }!!
}

fun drawSeam(pixel: Pixel, image: BufferedImage) {

    var curPixel: Pixel? = pixel
    while (curPixel != null) {
        image.setRGB(curPixel.x, curPixel.y, Color(255, 0, 0).rgb)
        curPixel = curPixel.prev
    }
}

fun compressVertical(pixel: Pixel, image: BufferedImage) : BufferedImage {

    val seamCoords = mutableMapOf<Int, Int>()
    var curPixel: Pixel? = pixel
    while (curPixel != null) {
        seamCoords.put(curPixel.y,  curPixel.x)
        curPixel = curPixel.prev
    }

    val unseamedImage = BufferedImage(image.width - 1, image.height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            if (x < seamCoords[y]!!) {
                unseamedImage.setRGB(x, y, image.getRGB(x, y))
            } else if (x > seamCoords[y]!!) {
                unseamedImage.setRGB(x - 1, y, image.getRGB(x, y))
            }
        }
    }
    return unseamedImage
}

fun compressHorizontal(pixel: Pixel, image: BufferedImage) : BufferedImage {

    val seamCoords = mutableMapOf<Int, Int>()
    var curPixel: Pixel? = pixel
    while (curPixel != null) {
        seamCoords.put(curPixel.x,  curPixel.y)
        curPixel = curPixel.prev
    }

    val unseamedImage = BufferedImage(image.width, image.height - 1, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            if (y < seamCoords[x]!!) {
                unseamedImage.setRGB(x, y, image.getRGB(x, y))
            } else if (y > seamCoords[x]!!) {
                unseamedImage.setRGB(x, y - 1, image.getRGB(x, y))
            }
        }
    }
    return unseamedImage
}

fun debugging(pixelsEnergy: Array<DoubleArray>, fileName: String) {
    val dotIndex = fileName.lastIndexOf('.')
    val resultFileName = fileName.substring(0, dotIndex - 1) + "_debug_${LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)}.txt"
    val matrixEnergyFile = File(resultFileName)
    val writer = BufferedWriter(FileWriter(matrixEnergyFile))

    writer.use {
        for (y in pixelsEnergy.first().indices) {
            val rowListEnergy = mutableListOf<String>()
            for (x in pixelsEnergy.indices) {
                val energy = pixelsEnergy[x][y]
                val formatter = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
                formatter.applyPattern("0000.0000")
                val formattedEnergy = formatter.format(energy)
                print("$formattedEnergy |")
                rowListEnergy.add(formattedEnergy)
            }
            println()
            val joinToString = rowListEnergy.joinToString(" | ")
            writer.write(joinToString)
            writer.newLine()
        }
    }
}