package de.jfd.straingauge

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class Archive(val days: List<DayEntry>, val scans: List<ScanEntry>, val directory: File)
object BackupJson {
    fun day(d: DayEntry) = JSONObject().apply {
        put("date",d.date); put("values",JSONArray(d.values().map { it ?: JSONObject.NULL }))
        put("factors",d.factors); put("scanId",d.scanId ?: JSONObject.NULL)
        put("detected",d.detected ?: JSONObject.NULL); put("updatedAt",d.updatedAt)
        put("custom1",d.custom1); put("custom2",d.custom2)
    }
    fun parseDays(array: JSONArray): List<DayEntry> {
        require(array.length() <= 100000) { tr(R.string.backup_too_many_entries) }
        val days = (0 until array.length()).map { i ->
            val j=array.getJSONObject(i); val a=j.getJSONArray("values")
            require(a.length()==4) { tr(R.string.backup_four_scale_values_are_required) }
            val v=(0..3).map { if (a.isNull(it)) null else a.getDouble(it) }
            DayEntry(j.getString("date"),v[0],v[1],v[2],v[3],j.getInt("factors"),
                if(j.isNull("scanId")) null else j.getString("scanId"),
                if(j.isNull("detected")) null else j.getString("detected"), j.getLong("updatedAt"),j.optString("custom1",""),j.optString("custom2","")).also { it.validate() }
        }
        require(days.map { it.date }.distinct().size == days.size) { tr(R.string.backup_duplicate_days_in_backup) }
        return days
    }
}
class BackupManager(private val context: Context, private val db: AppDatabase) {
    private val images = File(context.filesDir,"scans").apply { mkdirs() }
    suspend fun exportZip(uri: Uri) {
        val snapshot = db.withTransaction { db.store().days() to db.store().scans() }
        val files = snapshot.second.flatMap { listOf(it.original,it.aligned) }.distinct()
        val manifest = JSONObject().apply {
            put("format","strain-gauge"); put("version",2)
            put("createdAt",System.currentTimeMillis())
            put("days",JSONArray(snapshot.first.map { BackupJson.day(it) }))
            put("scans",JSONArray(snapshot.second.map { s -> JSONObject().apply {
                put("id",s.id);put("weekStart",s.weekStart);put("original",s.original)
                put("aligned",s.aligned);put("template",s.template);put("createdAt",s.createdAt);put("diagnostics",s.diagnostics)
            } }))
            put("sha256",JSONObject().apply { files.forEach { name ->
                val f=File(images,name); require(f.isFile) { tr(R.string.backup_scan_file_is_missing, name) }; put(name,sha(f))
            } })
        }
        val manifestBytes=manifest.toString(2).toByteArray()
        require(files.size<=10000 && manifestBytes.size<=8*1024*1024 && files.all { File(images,it).length()<=25L*1024*1024 } &&
            files.sumOf { File(images,it).length() }+manifestBytes.size<=250L*1024*1024) { tr(R.string.backup_backup_exceeds_the_supported_size_250_mb_in) }
        requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifestBytes); zip.closeEntry()
                files.forEach { name -> zip.putNextEntry(ZipEntry("images/${name}")); File(images,name).inputStream().use { it.copyTo(zip) };zip.closeEntry() }
            }
        }
    }
    suspend fun exportCsv(uri: Uri) {
        val days=db.store().days()
        requireNotNull(context.contentResolver.openOutputStream(uri,"wt")).bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("\uFEFFDatum;Anforderungen;Anspannung;Energie;Handlungsspielraum;Belastungsfaktoren\r\n")
            days.forEach { d ->
                val factors=d.selectedFactors().joinToString(" | ")
                w.write((listOf(d.date)+d.values().map { it?.toString() ?: "" }+listOf("\"${factors.replace("\"","\"\"")}\"")).joinToString(";")+"\r\n")
            }
        }
    }
    /** Untrusted archive: size bounds, flat names, hashes and references before any DB writes. */
    fun readZip(uri: Uri): Archive {
        val stage=File(context.cacheDir,"import-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            var total=0L;var count=0; val names=mutableSetOf<String>()
            ZipInputStream(requireNotNull(context.contentResolver.openInputStream(uri))).use { zip ->
                while(true) {
                    val entry=zip.nextEntry ?: break
                    require(!entry.isDirectory && ++count<=10001) { tr(R.string.backup_invalid_archive_or_too_many_files) }
                    val name=entry.name
                    require(name=="manifest.json" || name.matches(Regex("images/[A-Za-z0-9_-]+\\.jpg"))) { tr(R.string.backup_disallowed_archive_path) }
                    require(names.add(name)) { tr(R.string.backup_duplicate_archive_file) }
                    val target=File(stage,name);target.parentFile?.mkdirs()
                    target.outputStream().use { out ->
                        val bytes=ByteArray(8192);var local=0L
                        while(true) { val n=zip.read(bytes);if(n<0)break
                            local+=n;total+=n
                            require(local <= (if(name=="manifest.json") 8L else 25L)*1024*1024 && total<=250L*1024*1024) { tr(R.string.backup_backup_is_too_large_maximum_250_mb_in) }
                            out.write(bytes,0,n)
                        }
                    };zip.closeEntry()
                }
            }
            val root=JSONObject(File(stage,"manifest.json").readText())
            require(root.getString("format")=="strain-gauge" && root.getInt("version") in 1..2) { tr(R.string.backup_unknown_backup_format) }
            val days=BackupJson.parseDays(root.getJSONArray("days"))
            require(root.getInt("version")!=1 || days.all{it.factors<=255}) { tr(R.string.backup_invalid_factors_in_an_older_backup) }
            val a=root.getJSONArray("scans");require(a.length()<=5000)
            val scans=(0 until a.length()).map { i -> val j=a.getJSONObject(i)
                ScanEntry(j.getString("id"),j.getString("weekStart"),j.getString("original"),j.getString("aligned"),j.getString("template"),j.getLong("createdAt"),j.optString("diagnostics","[]"))
            }
            require(scans.map{it.id}.distinct().size==scans.size) { tr(R.string.backup_duplicate_scan_ids) }
            val expected=mutableSetOf("manifest.json")
            val hashes=root.getJSONObject("sha256")
            scans.forEach { s ->
                require(s.id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
                require(LocalDate.parse(s.weekStart).toString()==s.weekStart)
                listOf(s.original,s.aligned).forEach { name ->
                    require(name.matches(Regex("[A-Za-z0-9_-]+\\.jpg"))) { tr(R.string.backup_invalid_file_name) }
                    val f=File(stage,"images/${name}")
                    require(f.isFile && sha(f)==hashes.getString(name)) { tr(R.string.backup_a_photo_is_missing_or_its_checksum_does) }
                    expected.add("images/${name}")
                }
            }
            require(names==expected) { tr(R.string.backup_unexpected_files_in_backup) }
            val ids=scans.map{it.id}.toSet()
            require(days.all { it.scanId==null || it.scanId in ids }) { tr(R.string.backup_missing_scan_reference) }
            return Archive(days,scans,stage)
        } catch(e: Exception) { stage.deleteRecursively();throw e }
    }
    suspend fun importArchive(archive: Archive, replace: Boolean) {
        val made=mutableListOf<File>()
        try {
            db.withTransaction {
                val selected=Entries.merge(db.store().days(),archive.days,replace)
                val remap=mutableMapOf<String,String>()
                // Restore scans even if all their days were deleted. Reimporting
                // an identical backup must not duplicate its photos.
                fun fingerprint(s:ScanEntry, folder:File):String? {
                    val original=File(folder,s.original);val aligned=File(folder,s.aligned)
                    return if(original.isFile && aligned.isFile)
                        "${s.weekStart}|${s.template}|${s.diagnostics}|${sha(original)}|${sha(aligned)}" else null
                }
                val existingScans=db.store().scans()
                val byFingerprint=existingScans.mapNotNull { s -> fingerprint(s,images)?.let{it to s.id} }.toMap().toMutableMap()
                val usedIds=existingScans.map{it.id}.toMutableSet()
                val scans=archive.scans.mapNotNull { s ->
                    val fingerprint=requireNotNull(fingerprint(s,File(archive.directory,"images")))
                    val existingId=byFingerprint[fingerprint]
                    if(existingId!=null){remap[s.id]=existingId;return@mapNotNull null}
                    val id=if(s.id in usedIds)UUID.randomUUID().toString() else s.id
                    usedIds.add(id);remap[s.id]=id;byFingerprint[fingerprint]=id
                    val original="${id}-original.jpg";val aligned="${id}-aligned.jpg"
                    listOf(s.original to original,s.aligned to aligned).forEach { (old,new) ->
                        val file=File(images,new);require(!file.exists()) { tr(R.string.backup_target_file_already_exists) }
                        made+=file;File(archive.directory,"images/${old}").copyTo(file)
                    }
                    s.copy(id=id,original=original,aligned=aligned)
                }
                db.store().putScans(scans)
                db.store().putDays(selected.map{it.copy(scanId=it.scanId?.let{old -> remap.getValue(old)})})
            }
        } catch(e:Exception) { made.forEach{it.delete()};throw e }
        finally { archive.directory.deleteRecursively() }
    }
    private fun sha(file: File): String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)} }
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
}
