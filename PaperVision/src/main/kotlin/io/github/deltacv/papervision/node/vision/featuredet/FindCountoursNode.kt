package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.PointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.JavaTypes
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes.Imgproc
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes.Mat
import io.github.deltacv.papervision.codegen.build.type.OpenCvTypes.MatOfPoint
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_findcontours",
    category = Category.FEATURE_DET,
    description = "Finds all the contours (list of points) of a given binary image."
)
class FindContoursNode : DrawNode<FindContoursNode.Session>() {

    val inputMat = MatAttribute(INPUT, "$[att_binaryinput]")
    val outputPoints = ListAttribute(OUTPUT, PointsAttribute, "$[att_contours]")

    override fun onEnable() {
        + inputMat.rebuildOnChange()
        + outputPoints.rebuildOnChange()
    }

    override fun genCode(current: CodeGen.Current) = current {
        val session = Session()

        val input = inputMat.value(current)
        input.requireBinary(inputMat)

        val list = uniqueVariable("contours", JavaTypes.ArrayList(MatOfPoint).new())
        val hierarchyMat = uniqueVariable("hierarchy", Mat.new())

        group {
            private(list)
            private(hierarchyMat)
        }

        current.scope {
            list("clear")
            hierarchyMat("release")

            Imgproc("findContours", input.value, list, hierarchyMat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        }

        session.contoursList = GenValue.GList.RuntimeListOf(list, GenValue.GPoints.Points::class)

        session
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        if(attrib == outputPoints) {
            return lastGenSession!!.contoursList
        }

        noValue(attrib)
    }

    class Session : CodeGenSession {
        lateinit var contoursList: GenValue.GList.RuntimeListOf<GenValue.GPoints.Points>
    }

}