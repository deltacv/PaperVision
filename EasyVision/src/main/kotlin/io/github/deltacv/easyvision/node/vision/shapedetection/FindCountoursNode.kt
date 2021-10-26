package io.github.deltacv.easyvision.node.vision.shapedetection

import io.github.deltacv.easyvision.attribute.Attribute
import io.github.deltacv.easyvision.attribute.misc.ListAttribute
import io.github.deltacv.easyvision.attribute.vision.MatAttribute
import io.github.deltacv.easyvision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.easyvision.codegen.CodeGen
import io.github.deltacv.easyvision.codegen.CodeGenSession
import io.github.deltacv.easyvision.codegen.GenValue
import io.github.deltacv.easyvision.codegen.parse.new
import io.github.deltacv.easyvision.codegen.parse.v
import io.github.deltacv.easyvision.node.Category
import io.github.deltacv.easyvision.node.DrawNode
import io.github.deltacv.easyvision.node.RegisterNode

@RegisterNode(
    name = "nod_findcontours",
    category = Category.SHAPE_DET,
    description = "Finds all the contours (list of points) of a given binary image."
)
class FindContoursNode : DrawNode<FindContoursNode.Session>() {

    val inputMat = MatAttribute(INPUT, "Binary Input")
    val outputPoints = ListAttribute(OUTPUT, PointsAttribute, "Contours")

    override fun onEnable() {
        + inputMat
        + outputPoints
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputMat.value(current)
        input.requireBinary(inputMat)

        val listName = tryName("contours")
        val listValue = listName.v

        val hierarchyMatName = tryName("hierarchy")
        val hierarchyMatValue = hierarchyMatName.v

        import("org.opencv.imgproc.Imgproc")
        import("org.opencv.core.MatOfPoint")
        import("java.util.ArrayList")

        group {
            private(listName, new("ArrayList<MatOfPoint>"))
            private(hierarchyMatName, new("Mat"))
        }

        current.scope {
            "${listName}.clear"()
            "${hierarchyMatName}.release"()

            "Imgproc.findContours"(input.value, listValue, hierarchyMatValue, "Imgproc.RETR_LIST".v, "Imgproc.CHAIN_APPROX_SIMPLE".v)
        }

        session.contoursList = GenValue.GLists.RuntimeListOf(listValue, GenValue.GPoints.Points::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputPoints) {
            return genSession!!.contoursList
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var contoursList: GenValue.GLists.RuntimeListOf<GenValue.GPoints.Points>
    }

}