package io.github.deltacv.papervision.node.vision

enum class ColorSpace(val channels: Int, val channelNames: Array<String>) {
    RGBA(4,  arrayOf("R", "G", "B", "A")),
    RGB(3,   arrayOf("R", "G", "B")),
    BGR(3,   arrayOf("B", "G", "R")),
    HSV(3,   arrayOf("H", "S", "V")),
    YCrCb(3, arrayOf("Y", "Cr", "Cb")),
    LAB(3,   arrayOf("L", "A", "B")),
    GRAY(1,  arrayOf("Gray"))
}