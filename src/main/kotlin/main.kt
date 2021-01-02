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

    if (args.size < 4) {
        throw IllegalArgumentException("Wrong number of arguments. Required as next: -in sky.png -out sky-seam.png")
    }

    val (val1, val2, val3, val4) = args
    val inputFile: String
    val outputFile: String
    when {
        val1 == "-in" && val3 == "-out" -> {
            inputFile = val2
            outputFile = val4
        }
        val3 == "-in" && val1 == "-out" -> {
            inputFile = val4
            outputFile = val2
        }
        else -> {
            throw IllegalArgumentException("Wrong argument order. Required as next: -in sky.png -out sky-seam.png")
        }
    }

    val originalImage = ImageIO.read(Files.newInputStream(Paths.get(inputFile)))
    val (maxPixelEnergy, pixelsEnergy) = computeEnergy(originalImage)
    val minEnergyPath = minEnergyHorizontalPath(pixelsEnergy)
    drawSeam(minEnergyPath, originalImage)
    File(outputFile).let { file -> ImageIO.write(originalImage, file.extension, file) }
//    debugging(pixelsEnergy, outputFile)

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