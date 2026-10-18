package de.jfd.straingauge

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import org.json.JSONArray

enum class Screen { DASHBOARD, CAMERA, REVIEW, SETTINGS, HISTORY }
class AppModel(app: Application): AndroidViewModel(app) {
    val db=AppDatabase.open(app)
    private val store=db.store()
    private val backup=BackupManager(app,db)
    val days=store.observeDays().stateIn(viewModelScope,SharingStarted.Eagerly, emptyList())
    val scans=store.observeScans().stateIn(viewModelScope,SharingStarted.Eagerly, emptyList())
    var screen by mutableStateOf(Screen.DASHBOARD)
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var showDeleteOptions by mutableStateOf(false)
    var showDeleteConfirmation by mutableStateOf(false)
    var draft by mutableStateOf<ScanResult?>(null)
    var editedValues by mutableStateOf(List(7){List<Double?>(4){null}})
    var editedFactors by mutableStateOf(List(7){0})
    var editedCustom1 by mutableStateOf("")
    var editedCustom2 by mutableStateOf("")
    var week by mutableStateOf(monday().toString())
    var saveConflicts by mutableStateOf<List<String>?>(null)
    var archive by mutableStateOf<Archive?>(null)
    var importConflicts by mutableStateOf<List<String>>(emptyList())
    private var importedValues=emptyList<DayEntry>()
    fun runJob(action: suspend ()->Unit) {
        if(busy)return
        viewModelScope.launch {
            busy=true
            try { action() } catch(e:Exception) { message=e.message ?: "Vorgang fehlgeschlagen." }
            finally { busy=false }
        }
    }
    fun analyze(file: File) = runJob {
        val result=withContext(Dispatchers.Default){SheetScanner(getApplication()).analyze(file)}
        clearDraft()
        draft=result
        editedValues=(0..6).map { d -> (0..3).map { m -> result.values[m][d] } }
        editedFactors=result.factors
        editedCustom1="";editedCustom2="";changeWeek(monday().toString());screen=Screen.REVIEW
    }
    fun pickPhoto(uri: Uri) = runJob {
        val file=withContext(Dispatchers.IO){
            File.createTempFile("photo-",".jpg",getApplication<Application>().cacheDir).also { f ->
                requireNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use { input ->
                    f.outputStream().use { output ->
                        val buffer=ByteArray(8192);var total=0L
                        while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<35L*1024*1024){"Bild größer als 35 MB."};output.write(buffer,0,n)}
                    }
                }
            }
        }
        val result=withContext(Dispatchers.Default){SheetScanner(getApplication()).analyze(file)}
        clearDraft();draft=result
        editedValues=(0..6).map { d -> (0..3).map { m -> result.values[m][d] } }
        editedFactors=result.factors;editedCustom1="";editedCustom2="";changeWeek(monday().toString());screen=Screen.REVIEW
    }
    fun testScan() = runJob {
        val f=withContext(Dispatchers.IO){File.createTempFile("test-",".jpg",getApplication<Application>().cacheDir).also { file ->
            getApplication<Application>().assets.open("onepager_sample.jpg").use { input -> file.outputStream().use { input.copyTo(it) } }
        }}
        val result=withContext(Dispatchers.Default){SheetScanner(getApplication()).analyze(f)}
        clearDraft();draft=result
        editedValues=(0..6).map { d -> (0..3).map { m -> result.values[m][d] } }
        editedFactors=result.factors;editedCustom1="";editedCustom2="";changeWeek(monday().toString());screen=Screen.REVIEW
        editedCustom1="Testfaktor 1";editedCustom2="Testfaktor 2"
        message="Künstliches Testblatt. Speichern legt echte Einträge an; zum Testen kannst du abbrechen."
    }
    fun customLabels()=factorNames.take(8)+listOf(editedCustom1.ifBlank{factorNames[8]},editedCustom2.ifBlank{factorNames[9]})
    fun changeWeek(value:String) {
        week=value
        val start=runCatching{LocalDate.parse(value)}.getOrNull() ?: return
        val prior=days.value.firstOrNull { LocalDate.parse(it.date) in start..start.plusDays(6) }
        if(editedCustom1.isBlank())editedCustom1=prior?.custom1 ?: ""
        if(editedCustom2.isBlank())editedCustom2=prior?.custom2 ?: ""
    }
    fun setValue(day:Int, metric:Int,value:Double?) {
        editedValues=editedValues.mapIndexed { d,row -> if(d!=day)row else row.mapIndexed{m,v -> if(m==metric)value else v} }
    }
    fun setFactor(day:Int,factor:Int) {
        editedFactors=editedFactors.mapIndexed { d,mask -> if(d!=day)mask else mask xor (1 shl factor) }
    }
    private fun draftDays(id:String):List<DayEntry> {
        val start=LocalDate.parse(week)
        require(start.dayOfWeek==DayOfWeek.MONDAY){"Bitte den Montag als Wochenbeginn auswählen."}
        val result=requireNotNull(draft)
        return (0..6).mapNotNull { d ->
            val v=editedValues[d]
            if(v.all{it==null} && editedFactors[d]==0) null else
                DayEntry(start.plusDays(d.toLong()).toString(),v[0],v[1],v[2],v[3],editedFactors[d],id,
                    JSONArray((0..3).map { result.values[it][d] ?: org.json.JSONObject.NULL }).toString(),custom1=editedCustom1.trim(),custom2=editedCustom2.trim()).also{it.validate()}
        }
    }
    fun saveDraft(replace:Boolean?=null) = runJob {
        val id=UUID.randomUUID().toString();val incoming=draftDays(id)
        require(incoming.isNotEmpty()){ "Keine Werte vorhanden. Mindestens einen Wert setzen oder abbrechen." }
        val existing=withContext(Dispatchers.IO){store.days()}
        val overlaps=incoming.filter { n -> existing.any { it.date==n.date } }.map{it.date}
        if(replace==null && overlaps.isNotEmpty()){saveConflicts=overlaps;return@runJob}
        val selected=Entries.merge(existing,incoming,replace==true)
        if(selected.isEmpty()){saveConflicts=null;message="Vorhandene Daten beibehalten.";return@runJob}
        val result=requireNotNull(draft);val directory=File(getApplication<Application>().filesDir,"scans").apply{mkdirs()}
        val original=File(directory,"$id-original.jpg");val aligned=File(directory,"$id-aligned.jpg")
        withContext(Dispatchers.IO){
            try {
                // Re-encode: gallery input may be PNG/HEIC despite the temporary suffix.
                val b=loadOriented(result.original)
                try { original.outputStream().use { require(b.compress(android.graphics.Bitmap.CompressFormat.JPEG,94,it)) } } finally {b.recycle()}
                result.aligned.copyTo(aligned)
                db.withTransaction {
                    store.putScans(listOf(ScanEntry(id,week,original.name,aligned.name,template=result.templateId,diagnostics=JSONArray(result.warnings).toString())))
                    store.putDays(selected)
                }
            }catch(e:Exception){original.delete();aligned.delete();throw e}
        }
        saveConflicts=null;clearDraft();screen=Screen.DASHBOARD;message="${selected.size} Tage gespeichert."
    }
    fun clearDraft() {
        draft?.let { listOf(it.original,it.aligned,it.overlay).forEach { f -> if(f.parentFile==getApplication<Application>().cacheDir)f.delete() } }
        draft=null
    }
    fun export(uri:Uri,zip:Boolean)=runJob {
        withContext(Dispatchers.IO){if(zip)backup.exportZip(uri) else backup.exportCsv(uri)}
        message=if(zip)"Vollständige Sicherung exportiert." else "CSV exportiert."
    }
    fun exportBeforeDelete(uri: Uri) = runJob {
        message = null
        showDeleteConfirmation = false

        withContext(Dispatchers.IO) {
            backup.exportZip(uri)
        }

        // Diese Stelle wird nur nach erfolgreichem Export erreicht.
        showDeleteConfirmation = true
    }
    fun deleteAllData() = runJob {
        showDeleteOptions = false
        showDeleteConfirmation = false
        message = null

        val app = getApplication<Application>()

        val filesDeleted = withContext(Dispatchers.IO) {
            // Messwerte und Scan-Einträge aus der Datenbank entfernen.
            db.clearAllTables()

            // Inhalte löschen, die Verzeichnisse selbst behalten.
            fun emptyDirectory(directory: File): Boolean {
                if (!directory.exists()) return true

                val entries = directory.listFiles() ?: return false

                // Erst alle Löschversuche ausführen, dann das Ergebnis prüfen.
                return entries
                    .map { it.deleteRecursively() }
                    .all { it }
            }

            val photosDeleted = emptyDirectory(
                File(app.filesDir, "scans")
            )

            val cacheDeleted = emptyDirectory(app.cacheDir)

            photosDeleted && cacheDeleted
        }

        // Auch vorübergehend im Arbeitsspeicher gehaltene Daten zurücksetzen.
        clearDraft()
        cancelImport()

        saveConflicts = null
        importConflicts = emptyList()
        importedValues = emptyList()

        editedValues = List(7) { List<Double?>(4) { null } }
        editedFactors = List(7) { 0 }
        editedCustom1 = ""
        editedCustom2 = ""
        week = monday().toString()

        screen = Screen.DASHBOARD

        message = if (filesDeleted) {
            "Alle Messwerte und gespeicherten Scans wurden gelöscht."
        } else {
            "Die Datenbank wurde geleert. Einige Foto- oder " +
                    "Cache-Dateien konnten jedoch nicht gelöscht werden."
        }
    }
    fun inspectImport(uri:Uri)=runJob {
        val a=withContext(Dispatchers.IO){backup.readZip(uri)}
        archive?.directory?.deleteRecursively();archive=a
        importedValues=withContext(Dispatchers.IO){store.days()}
        importConflicts=Entries.conflicts(importedValues,a.days)
    }
    fun importArchive(replace:Boolean)=runJob {
        val a=requireNotNull(archive)
        withContext(Dispatchers.IO){backup.importArchive(a,replace)}
        archive=null;message="Import abgeschlossen.";screen=Screen.DASHBOARD
    }
    fun cancelImport(){archive?.directory?.deleteRecursively();archive=null}
    fun updateDay(day:DayEntry)=runJob {
        day.validate();withContext(Dispatchers.IO){store.putDays(listOf(day.copy(updatedAt=System.currentTimeMillis())))}
        message="Eintrag aktualisiert."
    }
    fun deleteDay(date:String)=runJob {withContext(Dispatchers.IO){store.deleteDay(date)};message="Tag gelöscht."}
    fun deleteScan(scan:ScanEntry)=runJob {
        withContext(Dispatchers.IO){
            db.withTransaction{store.unlinkScan(scan.id);store.deleteScan(scan.id)}
            listOf(scan.original,scan.aligned).forEach { File(getApplication<Application>().filesDir,"scans/$it").delete() }
        };message="Scan gelöscht; Tageswerte bleiben erhalten."
    }
    override fun onCleared(){clearDraft();cancelImport();db.close()}
}
fun monday(): LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
