package de.jfd.straingauge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import androidx.activity.enableEdgeToEdge

private val german=DateTimeFormatter.ofPattern("dd.MM.yyyy",Locale.GERMANY)
private fun displayDate(date:String)=runCatching{LocalDate.parse(date).format(german)}.getOrDefault(date)
private fun number(v:Double?)=v?.let{String.format(Locale.GERMANY,"%.1f",it)} ?: "–"
private fun factorsText(mask:Int,labels:List<String> = factorNames)=labels.filterIndexed{i,_->mask and (1 shl i)!=0}.joinToString(", ").ifEmpty{"keine"}
class MainActivity:ComponentActivity(){
    private val model:AppModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StrainGaugeTheme {
                App(model)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(model:AppModel){
    val days by model.days.collectAsStateWithLifecycle()
    val scans by model.scans.collectAsStateWithLifecycle()
    var discard by remember{mutableStateOf(false)}
    var photo by remember{mutableStateOf<File?>(null)}
    var edit by remember{mutableStateOf<DayEntry?>(null)}
    fun goBack(){if(model.screen==Screen.REVIEW)discard=true else model.screen=Screen.DASHBOARD}
    BackHandler(enabled=model.screen!=Screen.DASHBOARD || model.busy){if(!model.busy)goBack()}
    Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={
        TopAppBar(title={Text("Eine Woche im Blick",fontFamily=FontFamily.Serif)},
            navigationIcon={if(model.screen!=Screen.DASHBOARD)TextButton(onClick={goBack()},enabled=!model.busy){Text("Zurück")}},
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background
            ),
            actions={if(model.screen==Screen.DASHBOARD){
                TextButton(onClick={model.screen=Screen.HISTORY}){Text("Scans")}
                TextButton(onClick={model.screen=Screen.SETTINGS}){Text("Menü")}
            }})
    },floatingActionButton={if(model.screen==Screen.DASHBOARD)FloatingActionButton(onClick={model.screen=Screen.CAMERA},containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier=Modifier.semantics{contentDescription="Wochenblatt scannen"}){
        Text("+",style=MaterialTheme.typography.headlineLarge,modifier=Modifier.padding(horizontal=8.dp))
    }}){padding ->
        Box(Modifier.fillMaxSize().padding(padding)){
            when(model.screen){
                Screen.DASHBOARD -> Dashboard(days,onEdit={edit=it})
                Screen.CAMERA -> CameraScreen(model)
                Screen.REVIEW -> Review(model,onPhoto={photo=it})
                Screen.SETTINGS -> Settings(model)
                Screen.HISTORY -> History(scans,model,onPhoto={photo=it})
            }
        }
    }
    model.message?.let{message -> AlertDialog(onDismissRequest={model.message=null},title={Text("Hinweis")},text={Text(message)},confirmButton={TextButton(onClick={model.message=null}){Text("OK")}})}
    if(model.busy)Dialog(onDismissRequest={},properties=DialogProperties(dismissOnBackPress=false,dismissOnClickOutside=false)){
        Surface(shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){
            CircularProgressIndicator();Text("Einen Moment …")
        }}
    }
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Scan verwerfen?")},text={Text("Die noch nicht gespeicherten Werte werden verworfen.")},
        confirmButton={TextButton(onClick={model.clearDraft();model.screen=Screen.DASHBOARD;discard=false}){Text("Verwerfen")}},
        dismissButton={TextButton(onClick={discard=false}){Text("Weiter prüfen")}})
    model.saveConflicts?.let{ dates -> ConflictDialog("Tage bereits vorhanden",dates.map{date ->
        val old=days.first{it.date==date}
        val index=(LocalDate.parse(date).toEpochDay()-LocalDate.parse(model.week).toEpochDay()).toInt()
        "${displayDate(date)}\nBisher: ${old.values().joinToString(" / "){number(it)}}\nFaktoren: ${factorsText(old.factors,old.factorLabels())}\nScan: ${model.editedValues[index].joinToString(" / "){number(it)}}\nFaktoren: ${factorsText(model.editedFactors[index],model.customLabels())}"
    },
        onCancel={model.saveConflicts=null},onKeep={model.saveDraft(false)},onReplace={model.saveDraft(true)}) }
    model.archive?.let{a ->
        val old=days.associateBy{it.date}
        val conflictText=a.days.filter{it.date in model.importConflicts}.map{n ->
            val o=old.getValue(n.date)
            "${displayDate(n.date)}\nBisher: ${o.values().joinToString(" / "){number(it)}}\nFaktoren: ${factorsText(o.factors,o.factorLabels())}\nImport: ${n.values().joinToString(" / "){number(it)}}\nFaktoren: ${factorsText(n.factors,n.factorLabels())}"
        }
        ConflictDialog("Sicherung importieren",listOf("${a.days.size} Tage, ${a.scans.size} Scans.\n${a.days.count{it.date !in old}} neue Tage.\n${a.days.count{it.date in old}} Tage bereits vorhanden, davon ${model.importConflicts.size} mit abweichenden Werten.\nWerte: Anforderungen / Anspannung / Energie / Handlungsspielraum.")+conflictText,
            onCancel=model::cancelImport,onKeep={model.importArchive(false)},onReplace={model.importArchive(true)})
    }
    photo?.let{PhotoDialog(it,onClose={photo=null})}
    edit?.let{d -> DayEditor(d,onDismiss={edit=null},onSave={model.updateDay(it);edit=null},onDelete={model.deleteDay(d.date);edit=null})}
}

@Composable
fun Dashboard(all:List<DayEntry>,onEdit:(DayEntry)->Unit){
    val metricColors = metricColorsForTheme()
    var period by rememberSaveable{mutableIntStateOf(28)}
    var weekOffset by rememberSaveable{mutableIntStateOf(0)}
    val today=LocalDate.now()
    val end=maxOf(today,all.lastOrNull()?.let{LocalDate.parse(it.date)} ?: today)
    val start=if(period==0) all.firstOrNull()?.let{LocalDate.parse(it.date)} ?: end.minusDays(27) else end.minusDays(period.toLong()-1)
    val days=all.filter{LocalDate.parse(it.date)>=start && LocalDate.parse(it.date)<=end}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Spacer(Modifier.height(4.dp))
        Text("Dein Verlauf",fontFamily=FontFamily.Serif,style=MaterialTheme.typography.headlineLarge)
        Text("Was fordert dich? Was trägt dich?",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf(28 to "4 Wochen",90 to "3 Monate",0 to "Alles").forEach{(n,label)->FilterChip(selected=period==n,onClick={period=n},label={Text(label)})}
        }
        Text("${start.format(german)} – ${end.format(german)} · ${days.size} erfasste Tage",style=MaterialTheme.typography.labelMedium)
        if(all.isEmpty()){
            Card(colors=CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Deine erste Woche",style=MaterialTheme.typography.titleMedium)
                Text("Tippe unten rechts auf +, um dein Wochenblatt zu fotografieren. Im Menü findest du ein Testblatt zum Ausprobieren.")
            }}
        }
        metricNames.forEachIndexed{m,name ->
            val values=days.mapNotNull{it.values()[m]}
            Card(colors=CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),shape=RoundedCornerShape(18.dp)){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(name,style=MaterialTheme.typography.titleMedium,color=metricColors[m])
                        Text(if(values.isEmpty())"Keine Werte" else "Ø ${number(values.average())}",style=MaterialTheme.typography.labelLarge)
                    }
                    Plot(days,m,start,end,metricColors[m])
                    val latest=days.lastOrNull{it.values()[m]!=null}
                    Text(if(latest==null)"Noch kein Messwert" else "Zuletzt ${number(latest.values()[m])} · ${displayDate(latest.date)}",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        val selectedWeek=monday().plusWeeks(weekOffset.toLong())
        val weekDays=all.filter{LocalDate.parse(it.date)>=selectedWeek && LocalDate.parse(it.date)<=selectedWeek.plusDays(6)}
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("Wochenmittelwerte",fontFamily=FontFamily.Serif,style=MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    TextButton(onClick={weekOffset--}){Text("←")}
                    Text("Ab ${selectedWeek.format(german)}")
                    TextButton(onClick={weekOffset++}){Text("→")}
                }
                metricNames.forEachIndexed{m,name ->
                    val values=weekDays.mapNotNull{it.values()[m]}
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(name,modifier=Modifier.weight(1f))
                        Text("${if(values.isEmpty())"–" else number(values.average())} (${values.size} Tage)")
                    }
                }
            }
        }
        val counts=days.flatMap{it.selectedFactors()}.groupingBy{it}.eachCount()
        if(counts.isNotEmpty()){
            Text("Belastungsfaktoren",style=MaterialTheme.typography.titleLarge,fontFamily=FontFamily.Serif)
            counts.entries.sortedByDescending{it.value}.forEach{(label,count) ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,modifier=Modifier.weight(1f));Text("$count Tage")}
            }
        }
        if(days.isNotEmpty()){
            Text("Einträge bearbeiten",style=MaterialTheme.typography.titleLarge,fontFamily=FontFamily.Serif)
            Text("Reihenfolge: Anforderungen / Anspannung / Energie / Handlungsspielraum",style=MaterialTheme.typography.labelSmall)
            days.asReversed().forEach{d ->
                OutlinedCard(onClick={onEdit(d)},modifier=Modifier.fillMaxWidth()){
                    Column(Modifier.padding(14.dp)){
                        Text(displayDate(d.date),fontWeight=FontWeight.Bold)
                        Text(d.values().joinToString("   /   "){number(it)})
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun Plot(days:List<DayEntry>,metric:Int,start:LocalDate,end:LocalDate,color:Color){
    val axis = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(140.dp)){
        Column(Modifier.width(24.dp).fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween){listOf("10","5","0").forEach{Text(it,style=MaterialTheme.typography.labelSmall)}}
        Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical=6.dp)){
            val span=max(1L,end.toEpochDay()-start.toEpochDay()).toFloat()
            listOf(0f,.5f,1f).forEach{fraction ->drawLine(axis,Offset(0f,size.height*fraction),Offset(size.width,size.height*fraction),1.dp.toPx())}
            var previous:Pair<DayEntry,Offset>?=null
            days.forEach{day ->
                val value=day.values()[metric]
                if(value==null){previous=null;return@forEach}
                val point=Offset((LocalDate.parse(day.date).toEpochDay()-start.toEpochDay())/span*size.width,(1-value.toFloat()/10)*size.height)
                previous?.let{(before,p) ->
                    if(LocalDate.parse(day.date).toEpochDay()-LocalDate.parse(before.date).toEpochDay()==1L)drawLine(color,p,point,2.dp.toPx())
                }
                drawCircle(color,3.dp.toPx(),point);previous=day to point
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(start=24.dp),horizontalArrangement=Arrangement.SpaceBetween){
        Text(start.format(DateTimeFormatter.ofPattern("dd.MM.")),style=MaterialTheme.typography.labelSmall)
        Text(end.format(DateTimeFormatter.ofPattern("dd.MM.")),style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun Review(model:AppModel,onPhoto:(File)->Unit){
    val scan=model.draft ?: return
    var selected by rememberSaveable{mutableIntStateOf(0)}
    var original by rememberSaveable{mutableStateOf(false)}
    var showWarnings by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("Erkennen. Prüfen. Übernehmen.",fontFamily=FontFamily.Serif,style=MaterialTheme.typography.headlineSmall)
        Text(if(scan.templateId=="MNB-01/left")"OnePager · Linke Seite · Punktpositionen 0–10. Grün: erkannt; Orange: mehrdeutig. Werte bitte prüfen." else "BC-02 / Seite 1 · Kreismarkierungen 0–10. Werte bitte prüfen.")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=!original,onClick={original=false},label={Text("Erkennung")})
            FilterChip(selected=original,onClick={original=true},label={Text("Original")})
        }
        val file=if(original)scan.original else scan.overlay
        LocalPhoto(file,Modifier.fillMaxWidth().height(230.dp).clickable{onPhoto(file)})
        Text("Foto antippen zum Vergrößern und Zoomen.",style=MaterialTheme.typography.labelSmall)
        if(scan.warnings.isNotEmpty()){
            OutlinedButton(onClick={showWarnings=!showWarnings}){Text("${scan.warnings.size} Hinweise ${if(showWarnings)"verbergen" else "anzeigen"}")}
            if(showWarnings)scan.warnings.forEach{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
        }
        OutlinedTextField(value=model.week,onValueChange=model::changeWeek,label={Text("Montag der Woche (JJJJ-MM-TT)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            dayLabels.forEachIndexed{d,label -> FilterChip(selected=selected==d,onClick={selected=d},label={Text(label)})}
        }
        Text("${dayLabels[selected]} · Werte kontrollieren",style=MaterialTheme.typography.titleMedium)
        Text("Leere Felder bleiben fehlende Werte. Manuelle Korrekturen sind auch in 0,1-Schritten möglich.",style=MaterialTheme.typography.bodySmall)
        metricNames.forEachIndexed{m,name -> MetricEditor(name,model.editedValues[selected][m],{model.setValue(selected,m,it)})}
        Text("Belastungsfaktoren",style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(value=model.editedCustom1,onValueChange={model.editedCustom1=it.take(120)},label={Text("Name Zusatzfaktor 1 (für diese Woche)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=model.editedCustom2,onValueChange={model.editedCustom2=it.take(120)},label={Text("Name Zusatzfaktor 2 (für diese Woche)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Text("Handschriftliche Namen werden nicht gelesen. Bei markierten Zusatzzeilen bitte einen Namen eintragen.",style=MaterialTheme.typography.bodySmall)
        FactorEditor(model.editedFactors[selected],model.customLabels()){model.setFactor(selected,it)}
        Button(onClick={model.saveDraft()},enabled=!model.busy,modifier=Modifier.fillMaxWidth()){Text("Woche speichern")}
        Text("Es werden nur Tage mit mindestens einem Wert oder Belastungsfaktor übernommen.",style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun MetricEditor(name:String,value:Double?,onChange:(Double?)->Unit){
    Column{
        Row(verticalAlignment=Alignment.CenterVertically){
            Checkbox(checked=value!=null,onCheckedChange={onChange(if(it)5.0 else null)})
            Text(name,modifier=Modifier.weight(1f));Text(number(value),fontWeight=FontWeight.Bold)
        }
        if(value!=null)Slider(value=value.toFloat(),onValueChange={onChange(kotlin.math.round(it*10)/10.0)},valueRange=0f..10f,steps=99)
    }
}
@Composable
fun FactorEditor(mask:Int,labels:List<String> = factorNames,onToggle:(Int)->Unit){
    labels.forEachIndexed{f,label -> Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().clickable{onToggle(f)}){
        Checkbox(checked=mask and (1 shl f)!=0,onCheckedChange={onToggle(f)});Text(label,style=MaterialTheme.typography.bodyMedium)
    }}
    if(Integer.bitCount(mask)>3)Text("Mehr als drei Faktoren ausgewählt – bitte prüfen.",color=MaterialTheme.colorScheme.error)
}

@Composable
fun Settings(model:AppModel){
    val exportZip=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){it?.let{uri->model.export(uri,true)}}
    val exportCsv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let{uri->model.export(uri,false)}}
    val importZip=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(model::inspectImport)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Deine Daten",fontFamily=FontFamily.Serif,style=MaterialTheme.typography.headlineLarge)
        Text("Verarbeitung und Speicherung erfolgen auf diesem Smartphone. Die App benötigt keinen Internetzugang und erstellt keine automatische Cloud-Sicherung.")
        Button(onClick={exportZip.launch("Wochenblick-${LocalDate.now()}.zip")},modifier=Modifier.fillMaxWidth()){Text("Vollständige Sicherung exportieren")}
        OutlinedButton(onClick={importZip.launch(arrayOf("application/zip","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("Sicherung importieren")}
        Text("ZIP enthält Werte und Fotos. Vor dem Import werden Konflikte angezeigt. Exporte sind unverschlüsselt; wähle bewusst einen Speicherort.",style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        OutlinedButton(onClick={exportCsv.launch("Wochenblick-${LocalDate.now()}.csv")},modifier=Modifier.fillMaxWidth()){Text("Messwerte als CSV exportieren")}
        Text("Für Python und Excel: UTF-8, Semikolon, Dezimalpunkt. CSV ist ein Analyseexport; die Wiederherstellung erfolgt über ZIP.",style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Ausprobieren",style=MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick=model::testScan,modifier=Modifier.fillMaxWidth()){Text("Künstliches Testblatt auswerten")}
        Text("Öffnet ein künstlich ausgefülltes Blatt in der Scan-Prüfung. Erst Speichern übernimmt die Testwerte.",style=MaterialTheme.typography.bodySmall)
        Text("Vorlage: OnePager · linke Seite",style=MaterialTheme.typography.titleMedium)
        Text("Die neue OnePager-Vorlage wird anhand gedruckter Merkmale und aller vier L-Marken ausgerichtet. Die Punkte in den gestrichelten Kästchen liefern kontinuierliche Werte. Die frühere BC-02-Vorlage wird weiterhin erkannt.")
        Text("Version 0.2.0 · Persönliches Belastungs-Cockpit",style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun History(scans:List<ScanEntry>,model:AppModel,onPhoto:(File)->Unit){
    val context=LocalContext.current
    var deleting by remember{mutableStateOf<ScanEntry?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Deine Scans",fontFamily=FontFamily.Serif,style=MaterialTheme.typography.headlineLarge)
        if(scans.isEmpty())Text("Noch keine Scans gespeichert.")
        scans.forEach{s -> Card(colors=CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("Woche ab ${displayDate(s.weekStart)}",style=MaterialTheme.typography.titleMedium)
                Text(s.template,style=MaterialTheme.typography.labelSmall)
                if(s.diagnostics!="[]"){
                    val notes=remember(s.diagnostics){runCatching{
                        val array=org.json.JSONArray(s.diagnostics)
                        (0 until array.length()).joinToString("\n"){array.getString(it)}
                    }.getOrDefault("Erkennungshinweise konnten nicht gelesen werden.")}
                    Text("Hinweise bei der Aufnahme:\n$notes",style=MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    TextButton(onClick={onPhoto(File(context.filesDir,"scans/${s.original}"))}){Text("Original")}
                    TextButton(onClick={onPhoto(File(context.filesDir,"scans/${s.aligned}"))}){Text("Entzerrt")}
                    TextButton(onClick={deleting=s}){Text("Löschen")}
                }
            }
        }}
    }
    deleting?.let{s -> AlertDialog(onDismissRequest={deleting=null},title={Text("Scan löschen?")},text={Text("Die beiden Fotos werden gelöscht. Bereits erfasste Tageswerte bleiben erhalten.")},
        confirmButton={TextButton(onClick={model.deleteScan(s);deleting=null}){Text("Löschen")}},dismissButton={TextButton(onClick={deleting=null}){Text("Abbrechen")}})}
}

@Composable
fun ConflictDialog(title:String,lines:List<String>,onCancel:()->Unit,onKeep:()->Unit,onReplace:()->Unit){
    var replace by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onCancel,title={Text(title)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        lines.forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
        Text("Neue Tage werden ergänzt. Für bereits vorhandene Tage gilt:")
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=replace,onCheckedChange={replace=it});Text("Vorhandene Tage durch eingehende Werte ersetzen")}
        if(!replace)Text("Vorhandene Tage bleiben unverändert.")
    }},confirmButton={TextButton(onClick={if(replace)onReplace() else onKeep()}){Text("Übernehmen")}},dismissButton={TextButton(onClick=onCancel){Text("Abbrechen")}})
}

@Composable
fun DayEditor(day:DayEntry,onDismiss:()->Unit,onSave:(DayEntry)->Unit,onDelete:()->Unit){
    var values by remember(day){mutableStateOf(day.values())}
    var factors by remember(day){mutableIntStateOf(day.factors)}
    var custom1 by remember(day){mutableStateOf(day.custom1)}
    var custom2 by remember(day){mutableStateOf(day.custom2)}
    var delete by remember{mutableStateOf(false)}
    if(delete){AlertDialog(onDismissRequest={delete=false},title={Text("Tag löschen?")},text={Text("Die Werte vom ${displayDate(day.date)} werden gelöscht. Das Scan-Foto bleibt erhalten.")},
        confirmButton={TextButton(onClick=onDelete){Text("Löschen")}},dismissButton={TextButton(onClick={delete=false}){Text("Abbrechen")}});return}
    AlertDialog(onDismissRequest=onDismiss,title={Text(displayDate(day.date))},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())){
        metricNames.forEachIndexed{m,name -> MetricEditor(name,values[m]){v->values=values.mapIndexed{i,old->if(i==m)v else old}}}
        OutlinedTextField(value=custom1,onValueChange={custom1=it.take(120)},label={Text("Zusatzfaktor 1 für diesen Eintrag")},isError=factors and 256!=0 && custom1.isBlank(),singleLine=true)
        OutlinedTextField(value=custom2,onValueChange={custom2=it.take(120)},label={Text("Zusatzfaktor 2 für diesen Eintrag")},isError=factors and 512!=0 && custom2.isBlank(),singleLine=true)
        FactorEditor(factors,factorNames.take(8)+listOf(custom1.ifBlank{factorNames[8]},custom2.ifBlank{factorNames[9]})){f->factors=factors xor (1 shl f)}
        TextButton(onClick={delete=true}){Text("Tag löschen")}
    }},confirmButton={TextButton(enabled=(factors and 256==0 || custom1.isNotBlank()) && (factors and 512==0 || custom2.isNotBlank()),onClick={onSave(day.copy(demands=values[0],tension=values[1],energy=values[2],agency=values[3],factors=factors,custom1=custom1.trim(),custom2=custom2.trim()))}){Text("Speichern")}},dismissButton={TextButton(onClick=onDismiss){Text("Abbrechen")}})
}

@Composable
fun LocalPhoto(file:File,modifier:Modifier=Modifier){
    var failed by remember(file.path){mutableStateOf(false)}
    val bitmap by produceState<android.graphics.Bitmap?>(null,file.path){
        value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){runCatching{loadOriented(file)}.getOrNull()}
        failed=value==null
    }
    bitmap?.let{Image(it.asImageBitmap(),contentDescription="Arbeitsblatt",modifier=modifier,contentScale=ContentScale.Fit)} ?: Box(modifier,contentAlignment=Alignment.Center){Text(if(failed)"Foto konnte nicht geladen werden." else "Bild wird geladen …")}
}
@Composable
fun PhotoDialog(file:File,onClose:()->Unit){
    var zoom by remember{mutableFloatStateOf(1f)}
    var offset by remember{mutableStateOf(Offset.Zero)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                TextButton(onClick={zoom=1f;offset=Offset.Zero}){Text("Zurücksetzen")}
                TextButton(onClick=onClose){Text("Schließen")}
            }
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit){detectTransformGestures{_,pan,scale,_->
                zoom=(zoom*scale).coerceIn(1f,6f);offset=if(zoom<=1f)Offset.Zero else offset+pan
            }},contentAlignment=Alignment.Center){
                LocalPhoto(file,Modifier.fillMaxSize().graphicsLayer{scaleX=zoom;scaleY=zoom;translationX=offset.x;translationY=offset.y})
            }
            Text("Mit zwei Fingern zoomen und verschieben.",style=MaterialTheme.typography.labelSmall)
        }
    }
}
