package de.jfd.straingauge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Objdetect
import java.io.File
import kotlin.math.*

data class Grid(val x: List<Double>, val zero: Double, val ten: Double)
class FormProfile(context: Context) {
    private val json = JSONObject(context.assets.open("profile.json").bufferedReader().use { it.readText() })
    val width = json.getDouble("width")
    val height = json.getDouble("height")
    val markers = (0..3).map { val a = json.getJSONArray("markers").getJSONArray(it); Point(a.getDouble(0), a.getDouble(1)) }
    val grids = (0..3).map { val g = json.getJSONArray("grids").getJSONObject(it)
        Grid((0..6).map { d -> g.getJSONArray("x").getDouble(d) }, g.getDouble("yZero"), g.getDouble("yTen")) }
    val factorX = (0..6).map { json.getJSONObject("factors").getJSONArray("x").getDouble(it) }
    val factorY = (0..7).map { json.getJSONObject("factors").getJSONArray("y").getDouble(it) }
}
data class ScanResult(val original: File, val aligned: File, val overlay: File,
    val values: List<List<Double?>>, val factors: List<Int>, val warnings: List<String>, val templateId: String = "BC-02/01")

/** All recognition is local. Marker IDs fix orientation; no guess from page edges. */
class LegacySheetScanner(private val context: Context) {
    private val profile = FormProfile(context)
    fun analyze(source: File): ScanResult {
        check(OpenCVLoader.initLocal()) { tr(R.string.scan_opencv_could_not_be_loaded) }
        val bitmap = loadOriented(source)
        val rgba = Mat(); val gray = Mat(); val ids = Mat(); val corners = mutableListOf<Mat>()
        val warped = Mat(); val binary = Mat(); val annotated = Mat()
        val sourcePoints = MatOfPoint2f(); val targetPoints = MatOfPoint2f()
        var homography: Mat? = null
        try {
            Utils.bitmapToMat(bitmap, rgba); bitmap.recycle()
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)).detectMarkers(gray, corners, ids)
            val centers = mutableMapOf<Int, Point>()
            for (i in 0 until ids.rows()) {
                val id = ids.get(i, 0)[0].toInt()
                if (id in 0..3) {
                    require(!centers.containsKey(id)) { tr(R.string.scan_duplicate_marker_detected_please_photograph_only_one_sheet) }
                    centers[id] = Point((0..3).sumOf { corners[i].get(0, it)[0] } / 4,
                        (0..3).sumOf { corners[i].get(0, it)[1] } / 4)
                }
            }
            require(centers.keys.containsAll(listOf(0,1,2,3))) {
                tr(R.string.scan_the_four_corner_markers_on_the_first_page)
            }
            val scale = 3.0
            sourcePoints.fromList((0..3).map { centers.getValue(it) })
            targetPoints.fromList(profile.markers.map { Point(it.x * scale, it.y * scale) })
            homography = Imgproc.getPerspectiveTransform(sourcePoints, targetPoints)
            Imgproc.warpPerspective(gray, warped, homography, Size(round(profile.width*scale), round(profile.height*scale)),
                Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(255.0))
            Imgproc.adaptiveThreshold(warped, binary, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 41, 12.0)
            Imgproc.cvtColor(warped, annotated, Imgproc.COLOR_GRAY2RGB)
            fun fill(x: Double, y: Double): Double {
                val cx = (x*scale).roundToInt(); val cy = (y*scale).roundToInt()
                val radius = 4 // Inside the printed circle, not its outline.
                var count = 0; var dark = 0
                for (dy in -radius..radius) for (dx in -radius..radius) if (dx*dx+dy*dy <= radius*radius) {
                    count++; if (binary.get(cy+dy,cx+dx)[0] > 0) dark++
                }
                return dark.toDouble()/count
            }
            // Printed outlines must still be visible at the known positions.
            var visible = 0
            for (g in profile.grids) for (x in g.x) for (v in 0..10) {
                val y = g.zero+(g.ten-g.zero)*v/10
                if ((0..11).count { a ->
                    val t = a*Math.PI/6
                    warped.get((y*scale+7.5*sin(t)).roundToInt(), (x*scale+7.5*cos(t)).roundToInt())[0] < 210
                } >= 2) visible++
            }
            require(visible >= 190) { tr(R.string.scan_the_layout_does_not_match_bc_02_page) }
            val warnings = mutableListOf<String>()
            val values = profile.grids.mapIndexed { m, g -> (0..6).map { d ->
                val scores = (0..10).map { v -> fill(g.x[d], g.zero+(g.ten-g.zero)*v/10) }
                val marked = scores.indices.filter { scores[it] >= 0.48 }
                val weak = scores.any { it in 0.23..<0.48 }
                val value = if (marked.size == 1 && !weak) marked.single().toDouble() else null
                if (marked.size > 1 || weak) warnings += tr(R.string.scan_unclear_mark_please_check, metricNames[m], dayLabels[d])
                marked.forEach { v -> Imgproc.circle(annotated, Point(g.x[d]*scale,(g.zero+(g.ten-g.zero)*v/10)*scale),
                    12, if (value != null) Scalar(25.0,140.0,110.0) else Scalar(220.0,120.0,20.0), 3) }
                value
            } }
            val factors = (0..6).map { d ->
                var mask = 0
                profile.factorY.forEachIndexed { f, y ->
                    val score = fill(profile.factorX[d],y)
                    if (score >= 0.48) { mask = mask or (1 shl f)
                        Imgproc.circle(annotated,Point(profile.factorX[d]*scale,y*scale),12,Scalar(25.0,140.0,110.0),3) }
                    else if (score >= 0.23) warnings += tr(R.string.scan_please_check_stress_factor, dayLabels[d], f+1)
                }
                if (Integer.bitCount(mask)>3) warnings += tr(R.string.scan_more_than_three_stress_factors, dayLabels[d])
                mask
            }
            val aligned = File.createTempFile("aligned-", ".jpg", context.cacheDir)
            val overlay = File.createTempFile("overlay-", ".jpg", context.cacheDir)
            writeMat(warped,aligned); writeMat(annotated,overlay)
            return ScanResult(source,aligned,overlay,values,factors,warnings)
        } finally {
            rgba.release(); gray.release(); ids.release(); corners.forEach { it.release() }
            warped.release(); binary.release(); annotated.release(); sourcePoints.release(); targetPoints.release(); homography?.release()
        }
    }
    private fun writeMat(mat: Mat, file: File) {
        val b = Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat,b)
        file.outputStream().use { check(b.compress(Bitmap.CompressFormat.JPEG,94,it)) }; b.recycle()
    }
}
val dayLabels: List<String> get() = listOf(tr(R.string.day_mon), tr(R.string.day_tue), tr(R.string.day_wed), tr(R.string.day_thu), tr(R.string.day_fri), tr(R.string.day_sat), tr(R.string.day_sun))

/** Respect EXIF including mirrored orientations; cap processing memory. */
fun loadOriented(file: File): Bitmap {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, options)
    require(options.outWidth > 0 && options.outHeight > 0) { tr(R.string.scan_no_readable_image) }
    var sample = 1
    while (max(options.outWidth,options.outHeight)/sample > 3600) sample *= 2
    val b = BitmapFactory.decodeFile(file.path,BitmapFactory.Options().apply { inSampleSize=sample })
        ?: error(tr(R.string.scan_the_image_could_not_be_opened))
    val matrix = Matrix()
    when (ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f,1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f,-1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f,1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f,1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
    }
    if (matrix.isIdentity) return b
    return Bitmap.createBitmap(b,0,0,b.width,b.height,matrix,true).also { if (it !== b) b.recycle() }
}
