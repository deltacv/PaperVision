package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.math.RangeAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.KeyPointsAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.Resolvable
import io.github.deltacv.papervision.codegen.build.Value
import io.github.deltacv.papervision.codegen.build.Variable
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
import io.github.deltacv.papervision.codegen.dsl.ScopeContext
import io.github.deltacv.papervision.codegen.dsl.generatorsBuilder
import io.github.deltacv.papervision.codegen.language.interpreted.CPythonLanguage
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.codegen.resolved
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode

@PaperNode(
    name = "nod_blobdetector",
    category = Category.FEATURE_DET,
    description = "des_blobdetector"
)
class BlobDetectorNode : DrawNode<BlobDetectorNode.Session>() {

    val input = MatAttribute(INPUT, "$[att_input]")

    val area = RangeAttribute(INPUT, "$[att_area]")
    val threshold = RangeAttribute(INPUT, "$[att_threshold]")
    val circularity = RangeAttribute(INPUT, "$[att_circularity]") { it / 100.0}
    val convexity = RangeAttribute(INPUT, "$[att_convexity]") { it / 100.0}

    val output = KeyPointsAttribute(AttributeMode.OUTPUT, "$[att_keypoints]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + area
        area.useSliders = false
        area.min = 1
        area.max = Int.MAX_VALUE

        + threshold
        threshold.min = 1
        threshold.max = 255

        + circularity
        circularity.min = 1
        circularity.max = 100

        + convexity
        convexity.min = 1
        convexity.max = 100

        + output
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val inputValue = input.value(current).value

                val areaRangeValue = area.value(current)
                val thresholdRangeValue = threshold.value(current)
                val circularityRangeValue = circularity.value(current)
                val convexityRangeValue = convexity.value(current)

                val params = uniqueVariable("blobDetectorParams", JvmOpenCvTypes.SimpleBlobDetector.Params.new())

                val detector = uniqueVariable("detector",
                    JvmOpenCvTypes.SimpleBlobDetector.nullVal
                )
                val keyPoints = uniqueVariable("keyPoints",
                    JvmOpenCvTypes.MatOfKeyPoint.new()
                )

                val varPrefix = "blobDet"

                val minArea = uniqueVariable("${varPrefix}MinArea", float(areaRangeValue.min.value.v))
                val maxArea = uniqueVariable("${varPrefix}MaxArea", float(areaRangeValue.max.value.v))

                val minThreshold = uniqueVariable("${varPrefix}MinThreshold", float(thresholdRangeValue.min.value.v))
                val maxThreshold = uniqueVariable("${varPrefix}MaxThreshold", float(thresholdRangeValue.max.value.v))

                val minCircularity = uniqueVariable("${varPrefix}MinCircularity", float(circularityRangeValue.min.value.v))
                val maxCircularity = uniqueVariable("${varPrefix}MaxCircularity", float(circularityRangeValue.max.value.v))

                val minConvexity = uniqueVariable("${varPrefix}MinConvexity", float(convexityRangeValue.min.value.v))
                val maxConvexity = uniqueVariable("${varPrefix}MaxConvexity", float(convexityRangeValue.max.value.v))

                group {
                    // fyi with the indices;
                    // 0 = min, 1 = max
                    public(minArea, area.label(0))
                    public(maxArea, area.label(1))

                    public(minThreshold, threshold.label(0))
                    public(maxThreshold, threshold.label(1))

                    public(minCircularity, circularity.label(0))
                    public(maxCircularity, circularity.label(1))

                    public(minConvexity, convexity.label(0))
                    public(maxConvexity, convexity.label(1))
                }

                group {
                    private(params)
                    private(detector)
                    private(keyPoints)
                }

                initContext {
                    detector instanceSet JvmOpenCvTypes.SimpleBlobDetector.callValue("create", JvmOpenCvTypes.SimpleBlobDetector, params)
                }

                current.scope {
                    nameComment()

                    params("set_filterByArea", trueValue)
                    params("set_minArea", minArea)
                    params("set_maxArea", maxArea)

                    separate()

                    params("set_minThreshold", minThreshold)
                    params("set_maxThreshold", maxThreshold)

                    separate()

                    params("set_filterByCircularity", trueValue)
                    params("set_minCircularity", minCircularity)
                    params("set_maxCircularity", maxCircularity)

                    separate()

                    params("set_filterByConvexity", trueValue)
                    params("set_minConvexity", minConvexity)
                    params("set_maxConvexity", maxConvexity)

                    separate()

                    detector("setParams", params)

                    separate()

                    keyPoints("release")
                    detector("detect", inputValue.v, keyPoints)
                }

                session.output = GenValue.RuntimeKeyPoints(keyPoints.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            session
        }
    }

    override fun getOutputValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        return when(attrib) {
            output -> GenValue.RuntimeKeyPoints.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.RuntimeKeyPoints
    }
}