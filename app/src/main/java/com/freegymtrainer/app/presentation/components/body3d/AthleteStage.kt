package com.freegymtrainer.app.presentation.components.body3d

import android.content.Context
import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.TransformManager
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.FilamentInstance
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.freegymtrainer.app.presentation.components.motion.MotionDef
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

/*
 * A small Filament scene: the skinned athlete (assets/models/athlete.glb), a soft floor, lights and the gym
 * props of the current exercise. One stage = one engine; the Compose wrapper (Athlete3DCanvas) owns it.
 */
internal class AthleteStage(private val context: Context) {
    private val engine: Engine
    private val renderer: Renderer
    private val scene: Scene
    private val view: View
    private val camera: Camera
    private val cameraEntity: Int
    private val tm: TransformManager
    private val provider: UbershaderProvider
    private val loader: AssetLoader
    private val resources: ResourceLoader
    private val lights = IntArray(2)
    private var indirect: IndirectLight? = null
    private var swapChain: SwapChain? = null

    private var athlete: FilamentAsset? = null
    private var floor: FilamentAsset? = null
    private lateinit var poser: AthletePoser
    private lateinit var boneInstances: IntArray
    private lateinit var locals: Array<BoneLocal>
    private val matrix = FloatArray(16)
    private lateinit var props: PropKit

    private var width = 0
    private var height = 0
    private var motion: MotionDef? = null
    private var dirty = true
    private var mat: FloatArray? = null
    private var floorTop = 0f
    var exerciseName: String? = null
    private var last = FloatArray(4) { Float.NaN }

    /** True once the athlete model loaded (false if the asset is missing from the APK). */
    val ready: Boolean get() = athlete != null

    init {
        ensureNativeLibs()
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        view.scene = scene
        cameraEntity = EntityManager.get().create()
        camera = engine.createCamera(cameraEntity)
        camera.setExposure(16f, 1f / 125f, 100f)
        view.camera = camera
        tm = engine.transformManager

        // nicer edges and no post-process surprises
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply { enabled = true; sampleCount = 4 }
        view.isPostProcessingEnabled = true
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply { enabled = true; intensity = 0.9f }
        view.bloomOptions = view.bloomOptions.apply { enabled = false }
        // transparent background: the Compose side paints a gradient behind the TextureView
        view.blendMode = View.BlendMode.TRANSLUCENT
        renderer.clearOptions = renderer.clearOptions.apply { clear = true; clearColor = floatArrayOf(0f, 0f, 0f, 0f) }

        provider = UbershaderProvider(engine)
        loader = AssetLoader(engine, provider, EntityManager.get())
        resources = ResourceLoader(engine)
        setupLights()
        floor = loadGlb("models/props/floor.glb")?.also {
            scene.addEntities(it.entities)
            tm.setTransform(tm.getInstance(it.root), composeMatrix(V3.ZERO, Q.IDENTITY, FLOOR_RADIUS, 1f, FLOOR_RADIUS))
            it.instance.materialInstances.forEach { mi -> mi.setParameter("baseColorFactor", FLOOR_COLOR[0], FLOOR_COLOR[1], FLOOR_COLOR[2], FLOOR_COLOR[3]) }
        }
        props = PropKit(engine, scene, loader, resources, ::readAsset)
        athlete = loadGlb("models/athlete.glb")?.also { setupAthlete(it) }
    }

    private fun ensureNativeLibs() {
        if (!initialized) { Filament.init(); Gltfio.init(); initialized = true }
    }

    private fun readAsset(path: String): ByteBuffer? = try {
        val bytes = context.assets.open(path).use { it.readBytes() }
        ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).apply { flip() }
    } catch (e: java.io.IOException) {
        null
    }

    private fun loadGlb(path: String): FilamentAsset? {
        val buf = readAsset(path) ?: return null
        val asset = loader.createAsset(buf) ?: return null
        resources.loadResources(asset)
        asset.releaseSourceData()
        return asset
    }

    private fun setupLights() {
        // key light (casts the shadow) + softer fill; both are re-aimed with the camera each frame
        for (i in 0..1) lights[i] = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1f, 0.96f, 0.90f).intensity(110_000f).direction(0f, -1f, -1f).castShadows(true)
            .shadowOptions(LightManager.ShadowOptions().apply { mapSize = 1536; stable = false; shadowFar = 9f })
            .build(engine, lights[0])
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.80f, 0.88f, 1f).intensity(32_000f).direction(0f, -0.3f, 1f).castShadows(false)
            .build(engine, lights[1])
        scene.addEntity(lights[0]); scene.addEntity(lights[1])
        // soft sky / ground ambient from 2-band spherical harmonics
        val sh = floatArrayOf(
            0.95f, 0.98f, 1.05f,      // L00 (overall)
            0.30f, 0.32f, 0.36f,      // L1,-1 (y: brighter above)
            0f, 0f, 0f,
            0f, 0f, 0f
        )
        indirect = IndirectLight.Builder().irradiance(2, sh).intensity(28_000f).build(engine)
        scene.indirectLight = indirect
    }

    private fun setupAthlete(asset: FilamentAsset) {
        scene.addEntities(asset.entities)
        val inst = asset.instance
        val joints = inst.getJointsAt(0)
        val entityName = HashMap<Int, String>()
        for (e in joints) entityName[e] = asset.getName(e)
        val parentOf = HashMap<String, String?>()
        for (e in joints) {
            val p = tm.getParent(tm.getInstance(e))
            parentOf[entityName.getValue(e)] = entityName[p]
        }
        // parents first
        val order = ArrayList<String>()
        val remaining = LinkedHashSet(parentOf.keys)
        while (remaining.isNotEmpty()) {
            val next = remaining.filter { parentOf[it] == null || parentOf[it] in order }
            if (next.isEmpty()) break
            order += next; remaining.removeAll(next.toSet())
        }
        val entityOf = entityName.entries.associate { it.value to it.key }
        val rest = order.map { name ->
            val ti = tm.getInstance(entityOf.getValue(name))
            val local = tm.getTransform(ti, FloatArray(16))
            val world = tm.getWorldTransform(ti, FloatArray(16))
            RestBone(
                name, parentOf[name],
                V3(local[12], local[13], local[14]), Q.fromMatrix(local), V3(local[4], local[5], local[6]).len(),
                Q.fromMatrix(world), V3(world[12], world[13], world[14])
            )
        }
        poser = AthletePoser(rest)
        boneInstances = IntArray(rest.size) { tm.getInstance(entityOf.getValue(rest[it].name)) }
        locals = Array(rest.size) { BoneLocal(rest[it].localT, rest[it].localQ, rest[it].localS) }
    }

    // ---------------------------------------------------------------------------------------------

    fun setMotion(m: MotionDef) {
        motion = m
        mat = null
        dirty = true
        last.fill(Float.NaN)
    }

    fun onSurface(surface: Surface) {
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = engine.createSwapChain(surface)
        dirty = true
    }

    fun onSurfaceDestroyed() {
        swapChain?.let { engine.destroySwapChain(it); engine.flushAndWait() }
        swapChain = null
    }

    fun onResized(w: Int, h: Int) {
        width = w; height = h
        view.viewport = Viewport(0, 0, w, h)
        dirty = true
    }

    /** Re-draws on the next frame (e.g. after the theme changed). */
    fun invalidate() { dirty = true }

    /** Draws one frame. Returns false when nothing needed redrawing. */
    fun render(frameTimeNanos: Long, progress: Float, yawDeg: Float, pitchDeg: Float, glow: Float): Boolean {
        val chain = swapChain ?: return false
        val m = motion ?: return false
        if (width < 8 || height < 8) return false
        val state = floatArrayOf(progress, yawDeg, pitchDeg, glow)
        if (!dirty && state.contentEquals(last)) return false
        last = state; dirty = false

        if (athlete != null) {
            val sk = m.skeletonAt(progress)
            val hands = HandModes.handsFor(m, exerciseName)
            if (mat == null) prepareFloor(m, hands)
            poseBody(sk, hands, m.frontal)
            tm.openLocalTransformTransaction()
            for (i in locals.indices) {
                val b = locals[i]
                tm.setTransform(boneInstances[i], composeMatrix(b.t, b.q, b.s, matrix))
            }
            tm.commitLocalTransformTransaction()
            athlete!!.instance.animator.updateBoneMatrices()
            props.update(m, sk, poser, mat, exerciseName)
        }
        aimCamera(m, yawDeg, pitchDeg)
        if (renderer.beginFrame(chain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
        return true
    }

    /**
     * Solves the body for [sk] and keeps it out of the floor / mat: hands, feet, knees and elbows must rest on it
     * (the whole pose is raised just enough), a sinking head is tipped up instead (the neck bends), and as a last
     * resort a sinking trunk raises the pose too.
     */
    private fun poseBody(sk: com.freegymtrainer.app.presentation.components.motion.Skeleton, hands: Hands, frontal: Boolean) {
        var pose = sk.toPose3D()
        // the side-view data can't tell face-up from face-down; these are done lying on the stomach
        if (exerciseName in PRONE_EXERCISES) pose = pose.flippedFacing()
        fun solve() = poser.solve(pose, locals, hands, frontal)
        solve()
        val limbLow = poser.lowestLimb()
        if (limbLow < floorTop - 0.003f) { pose = pose.lifted((floorTop - limbLow) / AthletePoser.SCALE); solve() }
        val headLow = poser.lowestHead()
        if (headLow < floorTop - 0.003f) {
            pose = pose.withHead(pose.head + V3(0f, ((floorTop - headLow) / AthletePoser.SCALE).coerceAtMost(0.10f), 0f)); solve()
        }
        val trunkLow = poser.lowestTrunk()
        if (trunkLow < floorTop - 0.003f) { pose = pose.lifted((floorTop - trunkLow) / AthletePoser.SCALE); solve() }
    }

    /** Works out whether there is a mat and how far the body reaches over the whole movement. */
    private fun prepareFloor(m: MotionDef, hands: Hands) {
        floorTop = if (PropBuilder.hasMat(m)) PropBuilder.MAT_TOP else 0f
        val e = floatArrayOf(Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, 0f)
        for (i in 0 until 32) {
            poseBody(m.skeletonAt(i / 32f), hands, m.frontal)
            poser.extents(e)
        }
        mat = e
    }

    private fun aimCamera(m: MotionDef, yawDeg: Float, pitchDeg: Float) {
        val f = m.frame
        val s = AthletePoser.SCALE
        // Frame the athlete's real 3D height (hands raised overhead included) plus any tall equipment from the 2D frame.
        val top = max((f[3] - 0.10f) * s, (mat?.get(4) ?: 0f) + 0.16f)
        val bottom = min(f[2] * s, 0f)
        val cx = (f[0] + f[1]) / 2f
        // model space: pose x -> +Z, or -> +X for a frontal pose
        val target = if (m.frontal) V3(cx * s, (top + bottom) / 2f, 0f) else V3(-0f, (top + bottom) / 2f, cx * s)
        val extentH = top - bottom
        val extentW = max(f[1] - f[0], 0.9f) * s
        val fov = 30.0
        val aspect = width.toFloat() / height
        val tanHalf = tan(Math.toRadians(fov / 2)).toFloat()
        val dist = max(extentH * 0.58f / tanHalf, extentW * 0.54f / (tanHalf * aspect))

        val yaw = Math.toRadians((yawDeg + if (m.frontal) 90f else 0f).toDouble()).toFloat(); val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
        // pose-space direction from target to camera: yaw 0 = the side view from the near (+z) side
        val dx = -sin(yaw) * cos(pitch); val dy = sin(pitch); val dz = cos(yaw) * cos(pitch)
        val dirModel = if (m.frontal) V3(dx, dy, dz) else V3(-dz, dy, dx)
        val eye = target + dirModel * dist
        camera.setProjection(fov, aspect.toDouble(), 0.1, 40.0, Camera.Fov.VERTICAL)
        camera.lookAt(eye.x.toDouble(), eye.y.toDouble(), eye.z.toDouble(), target.x.toDouble(), target.y.toDouble(), target.z.toDouble(), 0.0, 1.0, 0.0)

        // lights ride with the camera so the front is always lit: key from above-right of the camera, fill from the left
        val toTarget = dirModel * -1f
        val right = toTarget.cross(V3.Y).unit(V3.X)
        val key = (toTarget + right * -0.55f + V3.Y * -0.85f).unit()
        val fill = (toTarget + right * 0.9f + V3.Y * -0.25f).unit()
        val lm = engine.lightManager
        lm.setDirection(lm.getInstance(lights[0]), key.x, key.y, key.z)
        lm.setDirection(lm.getInstance(lights[1]), fill.x, fill.y, fill.z)
        // the floor follows the focus point under the athlete
        floor?.let { tm.setTransform(tm.getInstance(it.root), composeMatrix(V3(target.x, 0f, target.z), Q.IDENTITY, FLOOR_RADIUS, 1f, FLOOR_RADIUS)) }
    }

    fun destroy() {
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
        props.destroy()
        athlete?.let { scene.removeEntities(it.entities); loader.destroyAsset(it) }
        floor?.let { scene.removeEntities(it.entities); loader.destroyAsset(it) }
        resources.destroy()
        loader.destroy()
        provider.destroyMaterials(); provider.destroy()
        for (l in lights) { scene.removeEntity(l); engine.lightManager.destroy(l); EntityManager.get().destroy(l) }
        indirect?.let { scene.indirectLight = null; engine.destroyIndirectLight(it) }
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyView(view); engine.destroyScene(scene); engine.destroyRenderer(renderer)
        engine.destroy()
    }

    companion object {
        private var initialized = false
        private val PRONE_EXERCISES = emptySet<String>()
        private const val FLOOR_RADIUS = 2.6f
        private val FLOOR_COLOR = floatArrayOf(0.085f, 0.09f, 0.12f, 1f)
    }
}
