/*
 * PaperVision
 * Copyright (C) 2026 Sebastian Erives, deltacv
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.deltacv.papervision.node.vision.featuredet

import io.github.deltacv.papervision.attribute.Attribute
import io.github.deltacv.papervision.attribute.AttributeMode
import io.github.deltacv.papervision.attribute.math.RangeAttribute
import io.github.deltacv.papervision.attribute.math.rebuildOnToggleChange
import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.rebuildOnChange
import io.github.deltacv.papervision.attribute.vision.MatAttribute
import io.github.deltacv.papervision.attribute.vision.structs.KeyPointAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.CodeGenSession
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.build.type.CPythonOpenCvTypes
import io.github.deltacv.papervision.codegen.build.type.JvmOpenCvTypes
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
    val circularity = RangeAttribute(INPUT, "$[att_circularity]") { it / 100.0 }
    val convexity = RangeAttribute(INPUT, "$[att_convexity]") { it / 100.0 }
    val inertia = RangeAttribute(INPUT, "$[att_inertia]") { it / 100.0 }

    val output = ListAttribute(AttributeMode.OUTPUT, KeyPointAttribute, "$[att_keypoints]")

    override fun onEnable() {
        + input.rebuildOnChange()

        + threshold
        threshold.min = 1
        threshold.max = 255

        + area.rebuildOnToggleChange()
        area.useSliders = false
        area.useToggle = true
        area.min = 1
        area.max = Int.MAX_VALUE

        + circularity.rebuildOnToggleChange()
        circularity.useToggle = true
        circularity.min = 1
        circularity.max = 100

        + convexity.rebuildOnToggleChange()
        convexity.useToggle = true
        convexity.min = 1
        convexity.max = 100

        + inertia.rebuildOnToggleChange()
        inertia.useToggle = true
        inertia.min = 1
        inertia.max = 100

        + output
    }

    override val generators = generatorsBuilder {
        generatorFor(JavaLanguage) {
            val session = Session()

            current {
                val inputValue = input.genValue(current).value

                val areaRangeValue = area.genValue(current)
                val thresholdRangeValue = threshold.genValue(current)
                val circularityRangeValue = circularity.genValue(current)
                val convexityRangeValue = convexity.genValue(current)
                val inertiaRangeValue = inertia.genValue(current)

                val params = uniqueVariable("blobDetectorParams", JvmOpenCvTypes.SimpleBlobDetector.Params.new())

                val detector = uniqueVariable("detector",
                    JvmOpenCvTypes.SimpleBlobDetector.nullValue
                )
                val keyPoints = uniqueVariable("keyPoints",
                    JvmOpenCvTypes.MatOfKeyPoint.new()
                )

                val pref = "blobDet"

                val minThreshold = uniqueVariable("${pref}MinThreshold", float(thresholdRangeValue.min.value.v))
                val maxThreshold = uniqueVariable("${pref}MaxThreshold", float(thresholdRangeValue.max.value.v))

                val minArea = uniqueVariable("${pref}MinArea", float(areaRangeValue.min.value.v))
                val maxArea = uniqueVariable("${pref}MaxArea", float(areaRangeValue.max.value.v))

                val minCircularity = uniqueVariable("${pref}MinCircularity", float(circularityRangeValue.min.value.v))
                val maxCircularity = uniqueVariable("${pref}MaxCircularity", float(circularityRangeValue.max.value.v))

                val minConvexity = uniqueVariable("${pref}MinConvexity", float(convexityRangeValue.min.value.v))
                val maxConvexity = uniqueVariable("${pref}MaxConvexity", float(convexityRangeValue.max.value.v))

                val minInertia = uniqueVariable("${pref}MinInertia", float(inertiaRangeValue.min.value.v))
                val maxInertia = uniqueVariable("${pref}MaxInertia", float(inertiaRangeValue.max.value.v))

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

                    public(minInertia, inertia.label(0))
                    public(maxInertia, inertia.label(1))
                }

                group {
                    private(params)
                    private(detector)
                    private(keyPoints)
                }

                initScope {
                    detector instanceSet JvmOpenCvTypes.SimpleBlobDetector.callValue("create", JvmOpenCvTypes.SimpleBlobDetector, params)
                }

                current.scope {
                    nameComment()

                    params("set_minThreshold", minThreshold)
                    params("set_maxThreshold", maxThreshold)

                    separate()

                    params("set_filterByArea", boolean(area.toggleValue.get()))
                    params("set_minArea", minArea)
                    params("set_maxArea", maxArea)

                    separate()

                    params("set_filterByCircularity", boolean(circularity.toggleValue.get()))
                    params("set_minCircularity", minCircularity)
                    params("set_maxCircularity", maxCircularity)

                    separate()

                    params("set_filterByConvexity", boolean(convexity.toggleValue.get()))
                    params("set_minConvexity", minConvexity)
                    params("set_maxConvexity", maxConvexity)

                    separate()

                    params("set_filterByInertia", boolean(inertia.toggleValue.get()))
                    params("set_minInertiaRatio", minInertia)
                    params("set_maxInertiaRatio", maxInertia)

                    separate()

                    detector("setParams", params)

                    separate()

                    keyPoints("release")
                    detector("detect", inputValue.v, keyPoints)
                }

                session.output = GenValue.GList.RuntimeListOf(keyPoints.resolved(), GenValue.GKeyPoint.RuntimeKeyPoint::class.resolved())
            }

            session
        }

        generatorFor(CPythonLanguage) {
            val session = Session()

            current {
                val inputValue = input.genValue(current).value

                val thresholdRangeValue = threshold.genValue(current)
                val areaRangeValue = area.genValue(current)
                val circularityRangeValue = circularity.genValue(current)
                val convexityRangeValue = convexity.genValue(current)
                val inertiaRangeValue = inertia.genValue(current)

                val params = uniqueVariable("blob_detector_params",
                    CPythonOpenCvTypes.cv2.callValue("SimpleBlobDetector_Params", CPythonLanguage.NoType)
                )

                val detector = uniqueVariable("blob_detector",
                    CPythonOpenCvTypes.cv2.callValue("SimpleBlobDetector_create", CPythonLanguage.NoType, params)
                )

                initScope {
                    local(params)

                    separate()

                    params.propertyVariable("minThreshold", CPythonLanguage.NoType) set float(thresholdRangeValue.min.value.v)
                    params.propertyVariable("maxThreshold", CPythonLanguage.NoType) set float(thresholdRangeValue.max.value.v)

                    separate()

                    params.propertyVariable("filterByArea", CPythonLanguage.NoType) set boolean(area.toggleValue.get())
                    params.propertyVariable("minArea", CPythonLanguage.NoType) set float(areaRangeValue.min.value.v)
                    params.propertyVariable("maxArea", CPythonLanguage.NoType) set float(areaRangeValue.max.value.v)

                    separate()

                    params.propertyVariable("filterByCircularity", CPythonLanguage.NoType) set boolean(circularity.toggleValue.get())
                    params.propertyVariable("minCircularity", CPythonLanguage.NoType) set float(circularityRangeValue.min.value.v)
                    params.propertyVariable("maxCircularity", CPythonLanguage.NoType) set float(circularityRangeValue.max.value.v)

                    separate()

                    params.propertyVariable("filterByConvexity", CPythonLanguage.NoType) set boolean(convexity.toggleValue.get())
                    params.propertyVariable("minConvexity", CPythonLanguage.NoType) set float(convexityRangeValue.min.value.v)
                    params.propertyVariable("maxConvexity", CPythonLanguage.NoType) set float(convexityRangeValue.max.value.v)

                    separate()

                    params.propertyVariable("filterByInertia", CPythonLanguage.NoType) set boolean(inertia.toggleValue.get())
                    params.propertyVariable("minInertiaRatio", CPythonLanguage.NoType) set float(inertiaRangeValue.min.value.v)
                    params.propertyVariable("maxInertiaRatio", CPythonLanguage.NoType) set float(inertiaRangeValue.max.value.v)

                    local(detector)
                }

                current.scope {
                    nameComment()

                    val keyPoints = uniqueVariable("keypoints", detector.callValue("detect", CPythonLanguage.NoType, inputValue.v))

                    local(keyPoints)

                    session.output = GenValue.GList.RuntimeListOf(keyPoints.resolved(), GenValue.GKeyPoint.RuntimeKeyPoint::class.resolved())
                }
            }

            session
        }
    }

    override fun getGenValueOf(current: CodeGen.Current, attrib: Attribute): GenValue {
        genCodeIfNecessary(current)

        return when(attrib) {
            output -> GenValue.GList.RuntimeListOf.defer { current.sessionOf(this)?.output }
            else -> noValue(attrib)
        }
    }

    class Session : CodeGenSession {
        lateinit var output: GenValue.GList.RuntimeListOf<GenValue.GKeyPoint.RuntimeKeyPoint>
    }
}
