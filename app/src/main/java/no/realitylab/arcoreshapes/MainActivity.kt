package no.realitylab.arcoreshapes

import android.app.ActivityManager
import android.content.Context
import android.graphics.Color.DKGRAY
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val MIN_OPENGL_VERSION = 3.0
    }

    lateinit var materialColor: Material
    lateinit var materialTexture: Material
    lateinit var customMaterial: Material

    lateinit var arFrameFragment: CustomArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish()) {
            return
        }

        setContentView(R.layout.activity_main)
        arFrameFragment = ar_fragment as CustomArFragment

        initMaterials()

        arFrameFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitResult.createAnchor()
            addToScene(anchor)
        }
    }

    private fun initMaterials() {
        MaterialFactory.makeOpaqueWithColor(this,
            Color(DKGRAY))
            .thenAccept {
                materialColor = it
            }

        Texture.builder()
            .setSource(this, R.drawable.texture)
            .build()
            .thenAccept { texture -> MaterialFactory.makeOpaqueWithTexture(this, texture)
                .thenAccept {
                    materialTexture = it
                }
            }

        ModelRenderable.builder()
            .setSource(this, Uri.parse("Mercury.sfb"))
            .build()
            .thenAccept { renderable ->
                customMaterial = renderable.material.makeCopy()
            }
    }

    private fun getSelectedMaterial(): Material {
        return when (chip_material_group.checkedChipId) {
            R.id.chip_color -> materialColor
            R.id.chip_texture -> materialTexture
            else -> customMaterial
        }
    }

    private fun addToScene(anchor: Anchor) {
        val size = 0.3f
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(arFrameFragment.transformationSystem)

        val material = getSelectedMaterial()
        val checkedId = chip_shape_group.checkedChipId
        node.renderable = when (checkedId) {
            R.id.chip_cube ->
                ShapeFactory.makeCube(
                    Vector3(size, size, size),
                    Vector3(0.0f, size / 2.0f, 0.0f),
                    material)
            R.id.chip_sphere ->
                ShapeFactory.makeSphere(
                    size,
                    Vector3(size / 2.0f, size / 2.0f, size / 2.0f),
                    material)
            else ->
                ShapeFactory.makeCylinder(
                    size,
                    size,
                    Vector3(0.0f, size / 2.0f, 0.0f),
                    material)
        }

        node.setParent(anchorNode)
        node.setOnTapListener { hitTestResult, motionEvent ->
            hitTestResult.node?.renderable?.material = getSelectedMaterial()
        }
        arFrameFragment.arSceneView.scene.addChild(anchorNode)
    }

    fun checkIsSupportedDeviceOrFinish() : Boolean {
        if (ArCoreApk.getInstance().checkAvailability(this) == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Toast.makeText(this, "ARFrames requires ARCore", Toast.LENGTH_LONG).show()
            finish()
            return false
        }
        val openGlVersionString =  (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
            ?.deviceConfigurationInfo
            ?.glEsVersion

        openGlVersionString?.let { s ->
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show()
                finish()
                return false
            }
        }
        return true
    }
}
