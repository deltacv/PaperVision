package io.github.deltacv.papervision.plugin.eocvsim

import org.opencv.core.Mat
import org.openftc.easyopencv.OpenCvPipeline
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luaj.LuaJ

class LuaOpenCvPipeline(val source: String) : OpenCvPipeline() {

    val interpreter = LuaJ()

    val initFunc by lazy { interpreter.get("init") }
    val processFrameFunc by lazy { interpreter.get("processFrame") }
    val onViewportTappedFunc by lazy { interpreter.get("onViewportTapped") }

    override fun init(mat: Mat?) {
        interpreter.eval(source)

        require(processFrameFunc.type() != Lua.LuaType.NIL) {
            "processFrame function not found in Lua script"
        }

        if(initFunc.type() != Lua.LuaType.NIL)
            initFunc.call(mat)

        println(source)
    }

    override fun processFrame(mat: Mat) = processFrameFunc.call(mat)[0].toJavaObject() as Mat?

    override fun onViewportTapped() {
        if(onViewportTappedFunc.type() != Lua.LuaType.NIL)
            onViewportTappedFunc.call()
    }

}