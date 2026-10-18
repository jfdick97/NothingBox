package de.jfd.straingauge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Objdetect
import java.io.File
import kotlin.math.*

/** Registration uses printed visual features, then confirms/refines all four L marks.
 * No OCR or network service: the top-left dot identifies the left page. */
class SheetScanner(private val context: Context) {
    fun analyze(source: File): ScanResult {
        check(OpenCVLoader.initLocal()) { "OpenCV konnte nicht geladen werden." }
        val bitmap=loadOriented(source)
        val rgba=Mat();val photo=Mat();val ids=Mat();val corners=mutableListOf<Mat>()
        try {
            Utils.bitmapToMat(bitmap,rgba);bitmap.recycle()
            Imgproc.cvtColor(rgba,photo,Imgproc.COLOR_RGBA2GRAY)
            // Keep already printed legacy forms usable, without changing saved records.
            ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50)).detectMarkers(photo,corners,ids)
            val legacy=(0 until ids.rows()).map{ids.get(it,0)[0].toInt()}.toSet()
            if(legacy.containsAll(listOf(0,1,2,3)))return LegacySheetScanner(context).analyze(source)
            return onePager(source,photo)
        } finally {rgba.release();photo.release();ids.release();corners.forEach{it.release()}}
    }
    private fun onePager(source:File,photo:Mat):ScanResult {
        val json=JSONObject(context.assets.open("onepager_profile.json").bufferedReader().use{it.readText()})
        val owned=mutableListOf<Mat>()
        fun mat()=Mat().also{owned+=it}
        fun pointMat(points:List<Point>)=MatOfPoint2f().also{it.fromList(points);owned+=it}
        val template=mat();val templateRgba=mat()
        val bitmap=context.assets.open("onepager_template.png").use{BitmapFactory.decodeStream(it)}
        requireNotNull(bitmap)
        Utils.bitmapToMat(bitmap,templateRgba);bitmap.recycle()
        Imgproc.cvtColor(templateRgba,template,Imgproc.COLOR_RGBA2GRAY)
        val referenceKeys=MatOfKeyPoint();val photoKeys=MatOfKeyPoint()
        val matches=mutableListOf<MatOfDMatch>()
        val orb=ORB.create(4500);val matcher=BFMatcher.create(Core.NORM_HAMMING,false)
        try {
            val referenceDesc=mat();val photoDesc=mat();val empty=mat()
            orb.detectAndCompute(template,empty,referenceKeys,referenceDesc)
            orb.detectAndCompute(photo,empty,photoKeys,photoDesc)
            require(!referenceDesc.empty() && !photoDesc.empty()){ "Zu wenig erkennbare Druckelemente. Bitte schärfer fotografieren." }
            matcher.knnMatch(referenceDesc,photoDesc,matches,2)
            val good=matches.mapNotNull{m->val pair=m.toArray();if(pair.size==2 && pair[0].distance<.72*pair[1].distance)pair[0] else null}
            require(good.size>=30){"OnePager nicht erkannt. Die linke Seite mit allen vier L-Marken vollständig aufnehmen."}
            val rk=referenceKeys.toArray();val pk=photoKeys.toArray()
            val from=pointMat(good.map{pk[it.trainIdx].pt});val to=pointMat(good.map{rk[it.queryIdx].pt})
            val mask=mat()
            val initial=Calib3d.findHomography(from,to,Calib3d.RANSAC,4.0,mask).also{owned+=it}
            require(!initial.empty() && Core.countNonZero(mask)>=30 && Core.countNonZero(mask)>=good.size*.25){"Das Foto lässt sich nicht sicher ausrichten."}
            val coarse=mat()
            Imgproc.warpPerspective(photo,coarse,initial,template.size(),Imgproc.INTER_LINEAR,Core.BORDER_CONSTANT,Scalar(255.0))
            val markers=(0..3).map{i->val a=json.getJSONArray("markers").getJSONArray(i);Point(a.getDouble(0)*3,a.getDouble(1)*3)}
            val found=(0..3).map{i->
                val a=json.getJSONArray("markerRects").getJSONArray(i)
                val r=Rect((a.getDouble(0)*3).roundToInt()-12,(a.getDouble(1)*3).roundToInt()-12,
                    (a.getDouble(2)*3).roundToInt()-(a.getDouble(0)*3).roundToInt()+24,
                    (a.getDouble(3)*3).roundToInt()-(a.getDouble(1)*3).roundToInt()+24)
                val (shift,score)=matchShift(coarse,template,r,24,24)
                require(score>=.65){"L-Marke ${i+1} nicht sicher erkannt. Alle Ecken frei halten und das Blatt eben legen."}
                Point(markers[i].x+shift.x,markers[i].y+shift.y)
            }
            val correction=Imgproc.getPerspectiveTransform(pointMat(found),pointMat(markers)).also{owned+=it}
            val transform=mat();Core.gemm(correction,initial,1.0,empty,0.0,transform)
            val aligned=mat();val binary=mat();val annotated=mat()
            Imgproc.warpPerspective(photo,aligned,transform,template.size(),Imgproc.INTER_LINEAR,Core.BORDER_CONSTANT,Scalar(255.0))
            Imgproc.adaptiveThreshold(aligned,binary,255.0,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV,41,12.0)
            Imgproc.cvtColor(aligned,annotated,Imgproc.COLOR_GRAY2RGB)
            fun fill(cx:Int,cy:Int,r:Int):Double {
                var n=0;var black=0
                for(dy in -r..r)for(dx in -r..r)if(dx*dx+dy*dy<=r*r){n++;if(binary.get(cy+dy,cx+dx)[0]>0)black++}
                return black.toDouble()/n
            }
            require(fill((20.8356*3).roundToInt(),(20.726*3).roundToInt(),3)>.45){"Orientierungspunkt oben links fehlt. Bitte die linke Seite fotografieren."}
            val warnings=mutableListOf<String>()
            val values=MutableList(4){MutableList<Double?>(7){null}}
            var confirmedFrames=0
            for(d in 0..6)for(m in 0..3){
                val a=json.getJSONArray("sliders").getJSONArray(d).getJSONArray(m)
                val x0=(a.getDouble(0)*3).roundToInt();val y0=(a.getDouble(1)*3).roundToInt()
                val x1=(a.getDouble(2)*3).roundToInt();val y1=(a.getDouble(3)*3).roundToInt()
                // Includes scale ticks below the box. Local registration compensates
                // small paper bends; the filled dot occupies only a small patch.
                val (shift,score)=matchShift(aligned,template,Rect(x0,y0-3,x1-x0,y1-y0+33),12,18)
                if(score<.45){warnings+="${dayLabels[d]}, ${metricNames[m]}: Skalenfeld nicht sicher erkannt.";continue}
                confirmedFrames++
                val dx=shift.x.roundToInt();val dy=shift.y.roundToInt()
                val left=x0+dx+4;val top=y0+dy+5
                val roi=binary.submat(Rect(left,top,x1-x0-8,y1-y0-10))
                val labels=Mat();val stats=Mat();val centers=Mat()
                try {
                    val count=Imgproc.connectedComponentsWithStats(roi,labels,stats,centers)
                    val candidates=(1 until count).filter{i->
                        val area=stats.get(i,Imgproc.CC_STAT_AREA)[0];val w=stats.get(i,Imgproc.CC_STAT_WIDTH)[0];val h=stats.get(i,Imgproc.CC_STAT_HEIGHT)[0]
                        area>=12 && area<=roi.total()*.3 && w in 5.0..36.0 && h>=5 && w/h in .35..2.8
                    }
                    if(candidates.size==1){
                        val i=candidates.single();val x=centers.get(i,0)[0]+left;val y=centers.get(i,1)[0]+top
                        val value=(10*(x-(x0+dx))/(x1-x0)).coerceIn(0.0,10.0)
                        values[m][d]=round(value*100)/100
                        Imgproc.circle(annotated,Point(x,y),12,Scalar(25.0,140.0,110.0),2)
                        Imgproc.putText(annotated,"%.1f".format(java.util.Locale.US,value),Point(x0+dx+5.0,y0+dy-5.0),Imgproc.FONT_HERSHEY_SIMPLEX,.45,Scalar(25.0,110.0,90.0),1)
                        if(stats.get(i,Imgproc.CC_STAT_LEFT)[0]<=1 || stats.get(i,Imgproc.CC_STAT_TOP)[0]<=0 ||
                            stats.get(i,Imgproc.CC_STAT_TOP)[0]+stats.get(i,Imgproc.CC_STAT_HEIGHT)[0]>=roi.rows()-1)
                            warnings+="${dayLabels[d]}, ${metricNames[m]}: Punkt nahe am Rahmen – Wert prüfen."
                    }else if(candidates.size>1){
                        warnings+="${dayLabels[d]}, ${metricNames[m]}: mehrere Punkte – bitte auswählen."
                        candidates.forEach{i->Imgproc.circle(annotated,Point(centers.get(i,0)[0]+left,centers.get(i,1)[0]+top),12,Scalar(220.0,120.0,20.0),2)}
                    }else if(Core.countNonZero(roi)>roi.total()*.015){warnings+="${dayLabels[d]}, ${metricNames[m]}: schwache/unklare Markierung prüfen."}
                } finally {roi.release();labels.release();stats.release();centers.release()}
            }
            require(confirmedFrames>=22){"Das Layout passt nicht zur linken OnePager-Seite oder ist zu unscharf."}
            val factors=(0..6).map{d->
                var bits=0
                for(f in 0..9){
                    val x=(json.getJSONArray("factorX").getDouble(d)*3).roundToInt()
                    val y=(json.getJSONArray("factorY").getDouble(f)*3).roundToInt()
                    val amount=fill(x,y,7)
                    if(amount>=.5){bits=bits or (1 shl f);Imgproc.circle(annotated,Point(x.toDouble(),y.toDouble()),15,Scalar(25.0,140.0,110.0),2)}
                    else if(amount>=.23){warnings+="${dayLabels[d]}: Belastungsfaktor ${f+1} prüfen."}
                }
                if(Integer.bitCount(bits)>3)warnings+="${dayLabels[d]}: mehr als drei Belastungsfaktoren."
                bits
            }
            val cleanFile=File.createTempFile("aligned-",".jpg",context.cacheDir)
            val overlayFile=File.createTempFile("overlay-",".jpg",context.cacheDir)
            write(aligned,cleanFile);write(annotated,overlayFile)
            return ScanResult(source,cleanFile,overlayFile,values,factors,warnings,"MNB-01/left")
        }finally{
            owned.forEach{it.release()};referenceKeys.release();photoKeys.release();matches.forEach{it.release()};orb.clear();matcher.clear()
        }
    }
    private fun matchShift(image:Mat,reference:Mat,rect:Rect,sx:Int,sy:Int):Pair<Point,Double> {
        val region=Rect(rect.x-sx,rect.y-sy,rect.width+2*sx,rect.height+2*sy)
        require(region.x>=0 && region.y>=0 && region.x+region.width<=image.cols() && region.y+region.height<=image.rows()){"Suchbereich außerhalb der Vorlage."}
        val patch=reference.submat(rect);val search=image.submat(region);val result=Mat()
        try {Imgproc.matchTemplate(search,patch,result,Imgproc.TM_CCOEFF_NORMED);val mm=Core.minMaxLoc(result)
            return Point(mm.maxLoc.x-sx,mm.maxLoc.y-sy) to mm.maxVal
        } finally {patch.release();search.release();result.release()}
    }
    private fun write(mat:Mat,file:File){
        val bitmap=Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);Utils.matToBitmap(mat,bitmap)
        try{file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,94,it))}}finally{bitmap.recycle()}
    }
}
