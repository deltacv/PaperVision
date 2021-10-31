/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.deltacv.easyvision

import com.github.serivesmejia.eocvsim.util.Log
import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import io.github.deltacv.easyvision.codegen.CodeGenManager
import io.github.deltacv.easyvision.gui.Font
import io.github.deltacv.easyvision.gui.FontManager
import io.github.deltacv.easyvision.gui.NodeEditor
import io.github.deltacv.easyvision.gui.NodeList
import io.github.deltacv.easyvision.gui.style.imnodes.ImNodesDarkStyle
import io.github.deltacv.easyvision.gui.util.PopupBuilder
import io.github.deltacv.easyvision.id.IdElementContainer
import io.github.deltacv.easyvision.io.KeyManager
import io.github.deltacv.easyvision.io.Keys
import io.github.deltacv.easyvision.node.NodeScanner
import io.github.deltacv.easyvision.platform.PlatformKeys
import io.github.deltacv.easyvision.platform.PlatformSetup
import io.github.deltacv.easyvision.platform.PlatformSetupCallback
import io.github.deltacv.easyvision.platform.PlatformWindow
import io.github.deltacv.easyvision.serialization.EasyVisionSerializer
import io.github.deltacv.easyvision.serialization.data.DataSerializer
import io.github.deltacv.mai18n.LangManager

class EasyVision(private val setupCall: PlatformSetupCallback) {

    companion object {
        const val TAG = "EasyVision"

        lateinit var platformKeys: PlatformKeys
            private set

        var imnodesStyle = ImNodesDarkStyle

        lateinit var defaultImGuiFont: Font
            private set

        val miscIds = IdElementContainer<Any>()
    }

    lateinit var setup: PlatformSetup
        private set
    lateinit var window: PlatformWindow
        private set

    val keyManager = KeyManager()
    val codeGenManager = CodeGenManager(this)
    val fontManager = FontManager()

    val langManager = LangManager("/lang.csv", "es").makeTr()

    val nodeEditor = NodeEditor(this, keyManager)
    val nodeList = NodeList(this, keyManager)

    lateinit var defaultFont: Font
        private set

    fun init() {
        for((key, values) in DataSerializer.deserialize(testJson)) {
            println("Deserialized: $key ->")
            for(value in values) {
                println("\t$value")
            }
        }

        Log.info(TAG, "Starting EasyVision...")
        Log.blank()

        NodeScanner.startAsyncScan()

        Log.info(TAG, "Using the ${setupCall.name} platform")
        setup = setupCall.setup()

        platformKeys = setup.keys ?: throw IllegalArgumentException("Platform ${setup.name} must provide PlatformKeys")
        window = setup.window ?: throw IllegalArgumentException("Platform ${setup.name} must provide a Window")

        nodeEditor.init()
        langManager.loadIfNeeded()

        // initializing fonts right after the imgui context is created
        // we can't create fonts mid-frame so that's kind of a problem
        defaultFont = fontManager.makeFont("/fonts/Calcutta-Regular.otf", "Calcutta", 13f)
        defaultImGuiFont = fontManager.makeDefaultFont(13f)

        nodeList.init()
    }

    fun firstProcess() {
        window.title = "EasyVision"
    }

    fun process() {
        ImGui.setNextWindowPos(0f, 0f, ImGuiCond.Always)

        val size = window.size
        ImGui.setNextWindowSize(size.x, size.y, ImGuiCond.Always)

        ImGui.pushFont(defaultFont.imfont)

        ImGui.begin("Editor",
            ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
                    or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoBringToFrontOnFocus
                    or ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoDecoration
        )

        nodeEditor.draw()

        ImGui.end()
        ImGui.popFont()

        nodeList.draw()

        ImGui.pushFont(defaultFont.imfont)
        PopupBuilder.draw()
        ImGui.popFont()

        if(ImGui.isMouseReleased(ImGuiMouseButton.Right)) {
            codeGenManager.build()
        }

        if(keyManager.pressed(Keys.ArrowUp)) {
            println(EasyVisionSerializer.serializeCurrent())
        }

        keyManager.update()
    }

    fun destroy() {
        nodeEditor.destroy()
    }
}

val testJson = """
{
  "nodes": [
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 0,
        "nodePos": {
          "x": 10.5,
          "y": 211.5
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.InputMatNode"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 1,
        "nodePos": {
          "x": 1260.5,
          "y": 308.5
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.OutputMatNode",
      "object": {
        "input": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 1
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        }
      }
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 2,
        "nodePos": {
          "x": 139.0,
          "y": 341.5
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.ThresholdNode",
      "object": {
        "input": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 2
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        },
        "scalar": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 3
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.structs.ScalarRangeAttribute"
        },
        "output": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 8
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        }
      }
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 3,
        "nodePos": {
          "x": 474.0,
          "y": 439.0
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.shapedetection.FindContoursNode",
      "object": {
        "inputMat": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 9
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        },
        "outputPoints": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 10
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.misc.ListAttribute"
        }
      }
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 4,
        "nodePos": {
          "x": 715.0,
          "y": 477.0
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.shapedetection.BoundingRectsNode",
      "object": {
        "inputContours": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 11
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.misc.ListAttribute"
        },
        "outputRects": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 12
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.misc.ListAttribute"
        }
      }
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 5,
        "nodePos": {
          "x": 717.0,
          "y": 146.0
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.overlay.DrawContoursNode",
      "object": {
        "inputMat": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 13
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        },
        "contours": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 14
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.misc.ListAttribute"
        },
        "lineColor": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 15
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.structs.ScalarAttribute"
        },
        "lineThickness": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 19
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.math.IntAttribute"
        },
        "outputMat": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 20
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        },
        "yes": 0
      }
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicNodeData",
      "data": {
        "id": 6,
        "nodePos": {
          "x": 1068.0,
          "y": 160.0
        }
      },
      "objectClass": "io.github.deltacv.easyvision.node.vision.overlay.DrawRectanglesNode",
      "object": {
        "inputMat": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 21
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        },
        "contours": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 22
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.misc.ListAttribute"
        },
        "lineColor": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 23
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.structs.ScalarAttribute"
        },
        "lineThickness": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 27
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.math.IntAttribute"
        },
        "outputMat": {
          "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.BasicAttribData",
          "data": {
            "id": 28
          },
          "objectClass": "io.github.deltacv.easyvision.attribute.vision.MatAttribute"
        }
      }
    }
  ],
  "links": [
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 0,
        "to": 2
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 8,
        "to": 9
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 10,
        "to": 11
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 10,
        "to": 14
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 0,
        "to": 13
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 20,
        "to": 21
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 28,
        "to": 1
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    },
    {
      "dataClass": "io.github.deltacv.easyvision.serialization.data.interfaces.LinkSerializationData",
      "data": {
        "from": 12,
        "to": 22
      },
      "objectClass": "io.github.deltacv.easyvision.node.Link"
    }
  ]
}
"""