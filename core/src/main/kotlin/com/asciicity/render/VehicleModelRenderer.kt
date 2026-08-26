package com.asciicity.render

import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import kotlin.math.cos
import kotlin.math.sin

/** Authoritative presentation input for the one vehicle-only 3D model pass. */
data class VehicleModelPose(
    val position: WorldPosition,
    val yawRadians: Double,
    val wheelRollRadians: Double,
    val frontWheelSteerRadians: Double,
)

data class VehicleModelFrame(
    val color: Texture,
    val depth: Texture,
)

/**
 * A deliberately small 3D island: it renders only the active vehicle to off-screen colour and
 * camera-forward-depth buffers. [AsciiPresenter] decides whether those pixels survive the ASCII
 * world-depth composite; this class never draws world geometry or UI.
 */
class VehicleModelRenderer(
    model: Model,
) : Disposable {
    private val instance = ModelInstance(model)
    private val camera = PerspectiveCamera()
    private val colorBatch = ModelBatch(VehicleFlatShaderProvider(encodeDepth = false))
    private val depthBatch = ModelBatch(VehicleFlatShaderProvider(encodeDepth = true))
    private val body = requiredNode("body")
    private val frontLeft = requiredNode("wheel_fl")
    private val frontRight = requiredNode("wheel_fr")
    private val rearLeft = requiredNode("wheel_rl")
    private val rearRight = requiredNode("wheel_rr")
    private var colorBuffer: FrameBuffer? = null
    private var depthBuffer: FrameBuffer? = null

    fun render(
        width: Int,
        height: Int,
        view: ViewPose,
        vehicle: VehicleModelPose?,
    ): VehicleModelFrame? {
        if (vehicle == null) return null
        require(width > 0 && height > 0) { "Vehicle target dimensions must be positive" }
        updateCamera(width, height, view)
        updatePose(vehicle)
        val color = ensureColorBuffer(width, height)
        val depth = ensureDepthBuffer(width, height)
        render(color, colorBatch)
        render(depth, depthBatch)
        return VehicleModelFrame(color.colorBufferTexture, depth.colorBufferTexture)
    }

    private fun updateCamera(width: Int, height: Int, view: ViewPose) {
        val pitchCos = cos(view.pitchRadians).toFloat()
        camera.fieldOfView = Math.toDegrees(
            2.0 * kotlin.math.atan(kotlin.math.tan(view.horizontalFovRadians * 0.5) / (width.toDouble() / height)),
        ).toFloat()
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.position.set(view.position.x.toFloat(), view.eyeHeight.toFloat(), view.position.z.toFloat())
        camera.direction.set(
            (sin(view.yawRadians) * pitchCos).toFloat(),
            sin(view.pitchRadians).toFloat(),
            (cos(view.yawRadians) * pitchCos).toFloat(),
        )
        camera.up.set(Vector3.Y)
        camera.near = 0.05f
        camera.far = MAXIMUM_DEPTH
        camera.update(true)
    }

    private fun updatePose(vehicle: VehicleModelPose) {
        body.translation.set(vehicle.position.x.toFloat(), 0f, vehicle.position.z.toFloat())
        body.rotation.setFromAxis(Vector3.Y, Math.toDegrees(vehicle.yawRadians).toFloat())
        applyWheelPose(frontLeft, vehicle.frontWheelSteerRadians, vehicle.wheelRollRadians)
        applyWheelPose(frontRight, vehicle.frontWheelSteerRadians, vehicle.wheelRollRadians)
        applyWheelPose(rearLeft, 0.0, vehicle.wheelRollRadians)
        applyWheelPose(rearRight, 0.0, vehicle.wheelRollRadians)
        instance.calculateTransforms()
    }

    private fun applyWheelPose(node: Node, steeringRadians: Double, rollRadians: Double) {
        node.rotation.set(VehicleWheelTransform.rotation(steeringRadians, rollRadians))
    }

    private fun render(buffer: FrameBuffer, batch: ModelBatch) {
        buffer.begin()
        try {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
            batch.begin(camera)
            batch.render(instance)
            batch.end()
        } finally {
            buffer.end()
        }
    }

    private fun ensureColorBuffer(width: Int, height: Int): FrameBuffer {
        val current = colorBuffer
        if (current != null && current.width == width && current.height == height) return current
        current?.dispose()
        return FrameBuffer(Pixmap.Format.RGBA8888, width, height, true).also { colorBuffer = it }
    }

    private fun ensureDepthBuffer(width: Int, height: Int): FrameBuffer {
        val current = depthBuffer
        if (current != null && current.width == width && current.height == height) return current
        current?.dispose()
        return FrameBuffer(Pixmap.Format.RGBA8888, width, height, true).also { depthBuffer = it }
    }

    private fun requiredNode(id: String): Node = checkNotNull(instance.getNode(id, true, false)) {
        "Vehicle model is missing required node '$id'"
    }

    override fun dispose() {
        colorBatch.dispose()
        depthBatch.dispose()
        colorBuffer?.dispose()
        depthBuffer?.dispose()
    }

    private companion object {
        private const val MAXIMUM_DEPTH = 48f
    }
}

internal object VehicleWheelTransform {
    /** Yaw the front axle first, then roll around the wheel's positive local X axis. */
    fun rotation(steeringRadians: Double, rollRadians: Double): Quaternion = Quaternion()
        .setFromAxis(Vector3.Y, Math.toDegrees(steeringRadians).toFloat())
        .mul(Quaternion().setFromAxis(Vector3.X, Math.toDegrees(rollRadians).toFloat()))
}

private class VehicleFlatShaderProvider(
    private val encodeDepth: Boolean,
) : ShaderProvider {
    private var shader: VehicleFlatShader? = null

    override fun getShader(renderable: Renderable): Shader = (shader ?: VehicleFlatShader(encodeDepth).also {
        it.init()
        shader = it
    }).also { check(it.canRender(renderable)) }

    override fun dispose() {
        shader?.dispose()
    }
}

private class VehicleFlatShader(
    private val encodeDepth: Boolean,
) : Shader {
    private val program = ShaderProgram(VERTEX_SHADER, if (encodeDepth) DEPTH_FRAGMENT_SHADER else COLOR_FRAGMENT_SHADER).also {
        check(it.isCompiled) { "Vehicle shader failed to compile: ${it.log}" }
    }
    private val diffuse = ColorAttribute(ColorAttribute.Diffuse)

    override fun init() = Unit

    override fun begin(camera: Camera, context: RenderContext) {
        program.bind()
        program.setUniformMatrix("u_projViewTrans", camera.combined)
        if (encodeDepth) program.setUniformMatrix("u_viewTrans", camera.view)
    }

    override fun render(renderable: Renderable) {
        program.setUniformMatrix("u_worldTrans", renderable.worldTransform)
        if (!encodeDepth) {
            val color = renderable.material.diffuseColor().color
            program.setUniformf("u_diffuse", color.r, color.g, color.b, color.a)
            val texture = renderable.material.get(TextureAttribute.Diffuse) as? TextureAttribute
            program.setUniformf("u_useDiffuseTexture", if (texture == null) 0f else 1f)
            texture?.textureDescription?.texture?.bind(0)
            if (texture != null) program.setUniformi("u_diffuseTexture", 0)
        }
        renderable.meshPart.render(program)
    }

    override fun end() = Unit

    override fun canRender(renderable: Renderable): Boolean = true

    override fun compareTo(other: Shader): Int = 0

    override fun dispose() {
        program.dispose()
    }

    private fun Material.diffuseColor(): ColorAttribute {
        val attribute = get(ColorAttribute.Diffuse) as? ColorAttribute
        return attribute ?: diffuse
    }

    private companion object {
        private const val VERTEX_SHADER = """
            attribute vec3 a_position;
            uniform mat4 u_projViewTrans;
            uniform mat4 u_viewTrans;
            uniform mat4 u_worldTrans;
            varying float v_forwardDepth;
            varying vec2 v_paintUv;

            void main() {
                vec4 worldPosition = u_worldTrans * vec4(a_position, 1.0);
                vec4 viewPosition = u_viewTrans * worldPosition;
                v_forwardDepth = -viewPosition.z;
                v_paintUv = a_position.xz + vec2(0.5);
                gl_Position = u_projViewTrans * worldPosition;
            }
        """

        private const val COLOR_FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            uniform vec4 u_diffuse;
            uniform sampler2D u_diffuseTexture;
            uniform float u_useDiffuseTexture;
            varying vec2 v_paintUv;
            void main() {
                vec3 paint = texture2D(u_diffuseTexture, v_paintUv).rgb;
                vec3 color = mix(u_diffuse.rgb, paint, u_useDiffuseTexture * 0.82);
                gl_FragColor = vec4(color, u_diffuse.a);
            }
        """

        private const val DEPTH_FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying float v_forwardDepth;
            void main() {
                float encoded = clamp(v_forwardDepth / 48.0, 0.0, 1.0);
                gl_FragColor = vec4(encoded, encoded, encoded, 1.0);
            }
        """
    }
}
