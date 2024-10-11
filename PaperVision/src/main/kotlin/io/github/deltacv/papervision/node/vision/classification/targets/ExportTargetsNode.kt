package io.github.deltacv.papervision.node.vision.classification.targets

import io.github.deltacv.papervision.attribute.misc.ListAttribute
import io.github.deltacv.papervision.attribute.misc.StringAttribute
import io.github.deltacv.papervision.attribute.vision.structs.RectAttribute
import io.github.deltacv.papervision.codegen.CodeGen
import io.github.deltacv.papervision.codegen.GenValue
import io.github.deltacv.papervision.codegen.NoSession
import io.github.deltacv.papervision.codegen.dsl.generators
import io.github.deltacv.papervision.codegen.dsl.targets
import io.github.deltacv.papervision.codegen.language.jvm.JavaLanguage
import io.github.deltacv.papervision.node.Category
import io.github.deltacv.papervision.node.DrawNode
import io.github.deltacv.papervision.node.PaperNode


@PaperNode(
    name = "nod_exporttargets",
    category = Category.CLASSIFICATION,
    description = "des_exporttargets"
)
class ExportTargetsNode : DrawNode<NoSession>() {

    val inputTargets = ListAttribute(INPUT, RectAttribute, "$[att_targets]")
    val label = StringAttribute(INPUT, "$[att_label]")

    override fun onEnable() {
        + inputTargets
        + label
    }

    override val generators = generators {
        generatorFor(JavaLanguage) {
            current {
                val targetsValue = inputTargets.value(current)

                if(targetsValue !is GenValue.GList.RuntimeListOf<*>) {
                    raise("") // TODO: Handle non-runtime lists
                }

                current.targets {
                    current.scope {
                        addTargets(string(label.value(current).value), targetsValue.value)
                    }
                }

                NoSession
            }
        }
    }

}