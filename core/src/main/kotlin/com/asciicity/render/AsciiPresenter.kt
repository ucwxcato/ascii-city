package com.asciicity.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.utils.Disposable

class AsciiPresenter(
    private val font: BitmapFont,
    private val worldFontProfile: AsciiFontProfile,
) : Disposable {
    private val batch = SpriteBatch()
    private val crtShader = ShaderProgram(CRT_VERTEX_SHADER, CRT_FRAGMENT_SHADER).also {
        check(it.isCompiled) { "CRT shader failed to compile: ${it.log}" }
    }
    private val crtBatch = SpriteBatch(1, crtShader)
    private val color = Color()
    private val contentTransform = Matrix4()
    private val identityTransform = Matrix4()
    private val contentProjection = Matrix4()
    private val glyphStrings = Array(127) { codePoint -> codePoint.toChar().toString() }
    private val whitePixel: Texture
    private val transparentPixel: Texture
    private var crtBuffer: FrameBuffer? = null
    private val compositeData = AsciiCompositeData()
    private var depthPixmap: Pixmap? = null
    private var emissivePixmap: Pixmap? = null
    private var depthTexture: Texture? = null
    private var emissiveTexture: Texture? = null

    var crtEffectEnabled: Boolean = false
        private set

    val compositeFiniteDepthCellCount: Int
        get() = compositeData.finiteDepthCellCount

    val compositeEmissiveCellCount: Int
        get() = compositeData.emissiveCellCount

    fun toggleCrtEffect() {
        crtEffectEnabled = !crtEffectEnabled
    }

    init {
        val pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixel.setColor(Color.WHITE)
        pixel.fill()
        whitePixel = Texture(pixel)
        whitePixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixel.setColor(0f, 0f, 0f, 0f)
        pixel.fill()
        transparentPixel = Texture(pixel)
        transparentPixel.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        pixel.dispose()
    }

    fun present(
        frame: AsciiFrame,
        viewport: AsciiViewport,
        projection: Matrix4,
        vehicleFrame: VehicleModelFrame? = null,
    ) {
        require(frame.width == viewport.columns && frame.height == viewport.rows) {
            "ASCII frame ${frame.width}x${frame.height} does not match viewport ${viewport.columns}x${viewport.rows}"
        }

        val contentWidth = viewport.pixelWidth
        val contentHeight = viewport.pixelHeight
        updateCompositeTextures(frame)
        val buffer = ensureCrtBuffer(contentWidth, contentHeight)
        buffer.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.projectionMatrix = contentProjection.setToOrtho2D(0f, 0f, contentWidth.toFloat(), contentHeight.toFloat())
        contentTransform
            .idt()
            .scale(viewport.glyphScale.toFloat(), viewport.glyphScale.toFloat(), 1f)
        batch.transformMatrix = contentTransform
        batch.begin()
        try {
            drawBackgrounds(frame, viewport)
            drawGlyphs(frame, viewport)
        } finally {
            batch.setColor(1f, 1f, 1f, 1f)
            font.setColor(Color.WHITE)
            batch.end()
            batch.transformMatrix = identityTransform
        }
        buffer.end()

        val activeDepthTexture = checkNotNull(depthTexture)
        val activeEmissiveTexture = checkNotNull(emissiveTexture)
        activeDepthTexture.bind(1)
        activeEmissiveTexture.bind(2)
        (vehicleFrame?.color ?: transparentPixel).bind(3)
        (vehicleFrame?.depth ?: transparentPixel).bind(4)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
        crtBatch.projectionMatrix = projection
        crtBatch.begin()
        try {
            crtShader.setUniformf("u_resolution", contentWidth.toFloat(), contentHeight.toFloat())
            crtShader.setUniformf("u_cellResolution", frame.width.toFloat(), frame.height.toFloat())
            crtShader.setUniformf("u_crtEnabled", if (crtEffectEnabled) 1f else 0f)
            crtShader.setUniformi("u_depthTexture", 1)
            crtShader.setUniformi("u_emissiveTexture", 2)
            crtShader.setUniformi("u_vehicleColor", 3)
            crtShader.setUniformi("u_vehicleDepth", 4)
            crtBatch.draw(
                buffer.colorBufferTexture,
                viewport.gutterX.toFloat(),
                viewport.gutterY.toFloat(),
                contentWidth.toFloat(),
                contentHeight.toFloat(),
                0,
                0,
                contentWidth,
                contentHeight,
                false,
                true,
            )
        } finally {
            crtBatch.end()
        }
    }

    private fun ensureCrtBuffer(width: Int, height: Int): FrameBuffer {
        val current = crtBuffer
        if (current != null && current.width == width && current.height == height) return current
        current?.dispose()
        return FrameBuffer(Pixmap.Format.RGBA8888, width, height, false).also { crtBuffer = it }
    }

    private fun updateCompositeTextures(frame: AsciiFrame) {
        compositeData.update(frame)
        ensureCompositeTextures(frame.width, frame.height)
        val depth = checkNotNull(depthPixmap)
        val emissive = checkNotNull(emissivePixmap)
        for (index in 0 until frame.cellCount) {
            val x = index % frame.width
            val y = index / frame.width
            depth.drawPixel(x, y, compositeData.depthPixelAt(index))
            emissive.drawPixel(x, y, compositeData.emissivePixelAt(index))
        }
        checkNotNull(depthTexture).draw(depth, 0, 0)
        checkNotNull(emissiveTexture).draw(emissive, 0, 0)
    }

    private fun ensureCompositeTextures(width: Int, height: Int) {
        if (depthPixmap?.width == width && depthPixmap?.height == height) return
        depthTexture?.dispose()
        emissiveTexture?.dispose()
        depthPixmap?.dispose()
        emissivePixmap?.dispose()
        depthPixmap = Pixmap(width, height, Pixmap.Format.RGBA8888).also {
            it.setBlending(Pixmap.Blending.None)
        }
        emissivePixmap = Pixmap(width, height, Pixmap.Format.RGBA8888).also {
            it.setBlending(Pixmap.Blending.None)
        }
        depthTexture = Texture(checkNotNull(depthPixmap)).also(::configureCellTexture)
        emissiveTexture = Texture(checkNotNull(emissivePixmap)).also(::configureCellTexture)
    }

    private fun configureCellTexture(texture: Texture) {
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    private fun drawBackgrounds(frame: AsciiFrame, viewport: AsciiViewport) {
        for (index in 0 until frame.cellCount) {
            val x = index % frame.width
            val y = index / frame.width
            setBatchColor(frame.backgroundAtIndex(index))
            batch.draw(
                whitePixel,
                (x * viewport.profile.cellWidth).toFloat(),
                (y * viewport.profile.cellHeight).toFloat(),
                viewport.profile.cellWidth.toFloat(),
                viewport.profile.cellHeight.toFloat(),
            )
        }
    }

    private fun drawGlyphs(frame: AsciiFrame, viewport: AsciiViewport) {
        for (index in 0 until frame.cellCount) {
            val glyph = frame.glyphAtIndex(index)
            if (glyph == ' '.code || glyph !in glyphStrings.indices) {
                continue
            }
            val x = index % frame.width
            val y = index / frame.width
            if (worldFontProfile != AsciiFontProfile.FINE || frame.emissiveAtIndex(index) > 0f) {
                drawPhosphorGlow(
                    glyph = glyph,
                    x = x * viewport.profile.cellWidth,
                    y = y * viewport.profile.cellHeight + viewport.profile.baselineFromCellBottom,
                    rgba = frame.foregroundAtIndex(index),
                )
            }
            setFontColor(frame.foregroundAtIndex(index))
            font.draw(
                batch,
                glyphStrings[glyph],
                (x * viewport.profile.cellWidth).toFloat(),
                (y * viewport.profile.cellHeight + viewport.profile.baselineFromCellBottom).toFloat(),
            )
        }
    }

    /**
     * A compact CRT treatment which deliberately stays in ASCII-space: the glyph grid remains
     * integer-aligned, while a restrained halo makes bright terminal characters feel emissive.
     */
    private fun drawPhosphorGlow(glyph: Int, x: Int, y: Int, rgba: Int) {
        Color.rgba8888ToColor(color, rgba)
        if (color.a == 0f) return
        color.a *= PHOSPHOR_GLOW_ALPHA
        font.setColor(color)
        val text = glyphStrings[glyph]
        font.draw(batch, text, (x - 1).toFloat(), y.toFloat())
        font.draw(batch, text, (x + 1).toFloat(), y.toFloat())
        font.draw(batch, text, x.toFloat(), (y - 1).toFloat())
        font.draw(batch, text, x.toFloat(), (y + 1).toFloat())
    }

    private fun setBatchColor(rgba: Int) {
        Color.rgba8888ToColor(color, rgba)
        batch.setColor(color)
    }

    private fun setFontColor(rgba: Int) {
        Color.rgba8888ToColor(color, rgba)
        font.setColor(color)
    }

    override fun dispose() {
        batch.dispose()
        crtBatch.dispose()
        crtShader.dispose()
        crtBuffer?.dispose()
        depthTexture?.dispose()
        emissiveTexture?.dispose()
        depthPixmap?.dispose()
        emissivePixmap?.dispose()
        whitePixel.dispose()
        transparentPixel.dispose()
    }

    private companion object {
        const val PHOSPHOR_GLOW_ALPHA = 0.10f

        private const val CRT_VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;

            void main() {
                v_color = a_color;
                v_texCoords = a_texCoord0;
                gl_Position = u_projTrans * a_position;
            }
        """

        private const val CRT_FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif

            varying vec4 v_color;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform sampler2D u_depthTexture;
            uniform sampler2D u_emissiveTexture;
            uniform sampler2D u_vehicleColor;
            uniform sampler2D u_vehicleDepth;
            uniform vec2 u_resolution;
            uniform vec2 u_cellResolution;
            uniform float u_crtEnabled;

            vec4 cellSample(sampler2D source, vec2 cell) {
                vec2 boundedCell = clamp(cell, vec2(0.0), u_cellResolution - vec2(1.0));
                return texture2D(source, (boundedCell + vec2(0.5)) / u_cellResolution);
            }

            float fartherEdge(vec4 currentDepth, vec4 neighborDepth) {
                float valid = step(0.5, currentDepth.a) * step(0.5, neighborDepth.a);
                return valid * smoothstep(0.018, 0.080, currentDepth.r - neighborDepth.r);
            }

            vec3 haloFromCell(vec2 cellPosition, vec2 sourceCell) {
                vec4 source = cellSample(u_emissiveTexture, sourceCell);
                float distanceInCells = length(cellPosition - sourceCell - vec2(0.5));
                float falloff = 1.0 - smoothstep(0.30, 1.42, distanceInCells);
                return source.rgb * source.a * falloff;
            }

            vec4 composedScene(vec2 uv) {
                vec4 ascii = texture2D(u_texture, uv);
                vec4 vehicle = texture2D(u_vehicleColor, uv);
                vec4 vehicleDepth = texture2D(u_vehicleDepth, uv);
                vec2 cell = min(floor(uv * u_cellResolution), u_cellResolution - vec2(1.0));
                vec4 asciiDepth = cellSample(u_depthTexture, cell);
                float vehicleIsNearer = step(0.5, vehicleDepth.a) *
                    max(1.0 - step(0.5, asciiDepth.a), step(vehicleDepth.r + 0.0015, asciiDepth.r));
                return mix(ascii, vehicle, vehicleIsNearer * vehicle.a);
            }

            void main() {
                vec2 centered = v_texCoords * 2.0 - 1.0;
                float radiusSquared = dot(centered, centered);
                vec2 curvedUv = centered * (1.0 + 0.075 * radiusSquared) * 0.5 + 0.5;
                float curvedInside = step(0.0, curvedUv.x) * step(curvedUv.x, 1.0) *
                    step(0.0, curvedUv.y) * step(curvedUv.y, 1.0);
                vec2 sampleUv = mix(v_texCoords, curvedUv, u_crtEnabled);
                float inside = mix(1.0, curvedInside, u_crtEnabled);

                vec2 chromaOffset = vec2(1.5 / u_resolution.x, 0.0) * (0.35 + radiusSquared) * u_crtEnabled;
                vec3 phosphor = vec3(
                    composedScene(sampleUv + chromaOffset).r,
                    composedScene(sampleUv).g,
                    composedScene(sampleUv - chromaOffset).b
                );
                vec3 halo = (
                    texture2D(u_texture, sampleUv + vec2(2.0 / u_resolution.x, 0.0)).rgb +
                    texture2D(u_texture, sampleUv - vec2(2.0 / u_resolution.x, 0.0)).rgb +
                    texture2D(u_texture, sampleUv + vec2(0.0, 2.0 / u_resolution.y)).rgb +
                    texture2D(u_texture, sampleUv - vec2(0.0, 2.0 / u_resolution.y)).rgb
                ) * 0.035;

                vec2 cellPosition = sampleUv * u_cellResolution;
                vec2 currentCell = min(floor(cellPosition), u_cellResolution - vec2(1.0));
                vec4 currentDepth = cellSample(u_depthTexture, currentCell);
                float contactEdge = 0.0;
                contactEdge = max(contactEdge, fartherEdge(currentDepth, cellSample(u_depthTexture, currentCell + vec2(-1.0, 0.0))));
                contactEdge = max(contactEdge, fartherEdge(currentDepth, cellSample(u_depthTexture, currentCell + vec2(1.0, 0.0))));
                contactEdge = max(contactEdge, fartherEdge(currentDepth, cellSample(u_depthTexture, currentCell + vec2(0.0, -1.0))));
                contactEdge = max(contactEdge, fartherEdge(currentDepth, cellSample(u_depthTexture, currentCell + vec2(0.0, 1.0))));
                float contactShade = 1.0 - contactEdge * 0.18;

                vec3 neonHaze = vec3(0.0);
                neonHaze += haloFromCell(cellPosition, currentCell + vec2(0.0, -1.0));
                neonHaze += haloFromCell(cellPosition, currentCell + vec2(-1.0, 0.0));
                neonHaze += haloFromCell(cellPosition, currentCell);
                neonHaze += haloFromCell(cellPosition, currentCell + vec2(1.0, 0.0));
                neonHaze += haloFromCell(cellPosition, currentCell + vec2(0.0, 1.0));
                neonHaze = min(neonHaze * 0.18, vec3(0.42));
                float scanline = mix(1.0, 0.78 + 0.22 * sin(v_texCoords.y * u_resolution.y * 3.14159265), u_crtEnabled);
                float vignette = mix(1.0, 1.0 - smoothstep(0.30, 1.30, radiusSquared) * 0.66, u_crtEnabled);
                gl_FragColor = vec4((phosphor * contactShade + halo + neonHaze) * scanline * vignette * inside, 1.0) * v_color;
            }
        """
    }
}
