package io.github.deltacv.easyvision.io

import org.lwjgl.glfw.GLFW.*

object Keys {

    val ArrowUp = glfwGetKeyScancode(GLFW_KEY_UP) //111
    val ArrowDown = glfwGetKeyScancode(GLFW_KEY_DOWN)// 116
    val ArrowLeft = glfwGetKeyScancode(GLFW_KEY_LEFT) // 113
    val ArrowRight = glfwGetKeyScancode(GLFW_KEY_RIGHT) //114

    val Escape =  glfwGetKeyScancode(GLFW_KEY_ESCAPE) //9
    val Spacebar = glfwGetKeyScancode(GLFW_KEY_SPACE) //65
    val Delete =  glfwGetKeyScancode(GLFW_KEY_DELETE) //119

    val LeftShift =  glfwGetKeyScancode(GLFW_KEY_LEFT_SHIFT) //50
    val RightShift = glfwGetKeyScancode(GLFW_KEY_RIGHT_SHIFT) //62

    val LeftControl = glfwGetKeyScancode(GLFW_KEY_LEFT_CONTROL) //37
    val RightControl = glfwGetKeyScancode(GLFW_KEY_RIGHT_SHIFT) //105

}