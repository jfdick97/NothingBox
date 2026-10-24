package de.jfd.straingauge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import androidx.activity.enableEdgeToEdge

private val german get() =DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(AppStrings.locale)
private fun displayDate(date:String)=runCatching{LocalDate.parse(date).format(german)}.getOrDefault(date)
private fun number(v:Double?)=v?.let{String.format(AppStrings.locale,"%.1f",it)} ?: "–"
private fun factorsText(mask:Int,labels:List<String> = factorNames)=labels.filterIndexed{i,_->mask and (1 shl i)!=0}.joinToString(", ").ifEmpty{tr(R.string.ui_none)}
class MainActivity:AppCompatActivity(){
    private val model:AppModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StrainGaugeTheme {
                App(model)
                ReminderStartupPermissions()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Reminders.scheduleNext(this)
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
    if (model.screen == Screen.CAMERA) {
        CameraScreen(
            model = model,
            onBack = { goBack() }
        )
    } else {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
            TopAppBar(
                title = { Text(tr(R.string.ui_my_nothing_box_strain_gauge), fontFamily = Garamond) },
                navigationIcon = {
                    if (model.screen != Screen.DASHBOARD)
                        AppIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                            label = tr(R.string.action_back),
                            onClick = { goBack() },
                            enabled = !model.busy
                        )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    if (model.screen == Screen.DASHBOARD) {
                        IconButton(
                            onClick = { model.screen = Screen.HISTORY }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = tr(R.string.ui_open_saved_scans)
                            )
                        }

                        IconButton(
                            onClick = { model.screen = Screen.SETTINGS }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = tr(R.string.ui_open_menu)
                            )
                        }
                    }
                })
        }, floatingActionButton = {
            if (model.screen == Screen.DASHBOARD) FloatingActionButton(
                onClick = { model.screen = Screen.CAMERA },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics { contentDescription = tr(R.string.camera_scan_weekly_sheet) }) {
                Text(
                    "+",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (model.screen) {
                    Screen.DASHBOARD -> Dashboard(
                        all = days,
                        onOpenEntries = {
                            model.screen = Screen.ENTRIES
                        }
                    )
                    Screen.CAMERA -> Unit
                    Screen.REVIEW -> Review(model, onPhoto = { photo = it })
                    Screen.SETTINGS -> Settings(model)
                    Screen.HISTORY -> History(scans, model, onPhoto = { photo = it })
                    Screen.ENTRIES -> EntriesScreen(
                        days = days,
                        onEdit = {
                            edit = it
                        },
                        onDeleteWeek = {
                            model.weekPendingDeletion = it.toString()
                        }
                    )
                }
            }
        }

        model.weekPendingDeletion?.let { value ->
            val selectedWeek = LocalDate.parse(value)

            AlertDialog(
                onDismissRequest = {
                    model.weekPendingDeletion = null
                },
                title = {
                    Text(tr(R.string.ui_delete_this_week))
                },
                text = {
                    Text(
                        tr(R.string.ui_all_daily_entries_for_this_week_will_be, weekRangeLabel(selectedWeek))
                    )
                },
                confirmButton = {
                    AppIconButton(
                        icon = Icons.Filled.DeleteForever,
                        label = tr(R.string.ui_permanently_delete_this_week),
                        onClick = {
                            model.deleteWeek(selectedWeek)
                        },
                        enabled = !model.busy,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                dismissButton = {
                    AppIconButton(
                        icon = Icons.Filled.Close,
                        label = tr(R.string.action_cancel),
                        onClick = {
                            model.weekPendingDeletion = null
                        }
                    )
                }
            )
        }
    }
    model.message?.let{message -> AlertDialog(onDismissRequest={model.message=null},title={Text(tr(R.string.ui_notice))},text={Text(message)},confirmButton={TextButton(onClick={model.message=null}){Text(tr(R.string.action_ok))}})}
    if(model.busy)Dialog(onDismissRequest={},properties=DialogProperties(dismissOnBackPress=false,dismissOnClickOutside=false)){
        Surface(shape=RoundedCornerShape(20.dp)){Row(Modifier.padding(28.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){
            CircularProgressIndicator();Text(tr(R.string.ui_one_moment))
        }}
    }
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text(tr(R.string.ui_discard_scan))},text={Text(tr(R.string.ui_values_that_have_not_been_saved_will_be))},
        confirmButton={TextButton(onClick={model.clearDraft();model.screen=Screen.DASHBOARD;discard=false}){Text(tr(R.string.ui_discard))}},
        dismissButton={TextButton(onClick={discard=false}){Text(tr(R.string.ui_continue_reviewing))}})
    model.saveConflicts?.let{ dates -> ConflictDialog(tr(R.string.ui_some_days_already_exist),dates.map{date ->
        val old=days.first{it.date==date}
        val index=(LocalDate.parse(date).toEpochDay()-LocalDate.parse(model.week).toEpochDay()).toInt()
        tr(R.string.ui_existing_factors_scan_factors, displayDate(date), old.values().joinToString(" / "){number(it)}, factorsText(old.factors,old.factorLabels()), model.editedValues[index].joinToString(" / "){number(it)}, factorsText(model.editedFactors[index],model.customLabels()))
    },
        onCancel={model.saveConflicts=null},onKeep={model.saveDraft(false)},onReplace={model.saveDraft(true)}) }
    model.archive?.let{a ->
        val old=days.associateBy{it.date}
        val conflictText=a.days.filter{it.date in model.importConflicts}.map{n ->
            val o=old.getValue(n.date)
            tr(R.string.ui_existing_factors_import_factors, displayDate(n.date), o.values().joinToString(" / "){number(it)}, factorsText(o.factors,o.factorLabels()), n.values().joinToString(" / "){number(it)}, factorsText(n.factors,n.factorLabels()))
        }
        ConflictDialog(tr(R.string.ui_import_backup),listOf(tr(R.string.ui_days_scans_new_days_days_already_exist_have, a.days.size, a.scans.size, a.days.count{it.date !in old}, a.days.count{it.date in old}, model.importConflicts.size))+conflictText,
            onCancel=model::cancelImport,onKeep={model.importArchive(false)},onReplace={model.importArchive(true)})
    }
    photo?.let{PhotoDialog(it,onClose={photo=null})}
    edit?.let{d -> DayEditor(d,onDismiss={edit=null},onSave={model.updateDay(it);edit=null},onDelete={model.deleteDay(d.date);edit=null})}
}

@Composable
fun Dashboard(all:List<DayEntry>,onOpenEntries: () -> Unit){
    val metricColors = metricColorsForTheme()
    var period by rememberSaveable{mutableIntStateOf(28)}
    var selectedWeekText by rememberSaveable {
        mutableStateOf(monday().toString())
    }
    val today=LocalDate.now()
    val trendAssessment = remember(all, today) {
        assessTrends(all, today)
    }
    val end=maxOf(today,all.lastOrNull()?.let{LocalDate.parse(it.date)} ?: today)
    val start=if(period==0) all.firstOrNull()?.let{LocalDate.parse(it.date)} ?: end.minusDays(27) else end.minusDays(period.toLong()-1)
    val days=all.filter{LocalDate.parse(it.date)>=start && LocalDate.parse(it.date)<=end}
    val dashboardContext = LocalContext.current

    val displayPreferences = remember(dashboardContext) {
        dashboardContext.getSharedPreferences(
            "display_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var showStressStatistics by rememberSaveable {
        mutableStateOf(
            displayPreferences.getBoolean(
                "show_stress_statistics",
                false
            )
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Spacer(Modifier.height(4.dp))
        Text(tr(R.string.ui_your_timeline),fontFamily=Garamond,style=MaterialTheme.typography.headlineLarge)
        Text(tr(R.string.ui_what_asks_a_lot_of_you_what_supports),color=MaterialTheme.colorScheme.onSurfaceVariant)
        TrendCard(trendAssessment)
        PeriodSelector(
            selectedPeriod = period,
            onPeriodSelected = { period = it }
        )
        Text(tr(R.string.ui_recorded_days, start.format(german), end.format(german), days.size),style=MaterialTheme.typography.labelMedium)
        if(all.isEmpty()){
            Card(colors=CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(tr(R.string.ui_your_first_week),style=MaterialTheme.typography.titleMedium)
                Text(tr(R.string.ui_tap_at_the_bottom_right_to_photograph_your))
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
                        Text(if(values.isEmpty())tr(R.string.ui_no_values) else "Ø ${number(values.average())}",style=MaterialTheme.typography.labelLarge)
                    }
                    Plot(days,m,start,end,metricColors[m])
                    val latest=days.lastOrNull{it.values()[m]!=null}
                    Text(if(latest==null)tr(R.string.ui_no_value_recorded_yet) else tr(R.string.ui_latest, number(latest.values()[m]), displayDate(latest.date)),style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tr(R.string.ui_stress_statistics),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )

            Switch(
                checked = showStressStatistics,
                onCheckedChange = { checked ->
                    showStressStatistics = checked

                    displayPreferences.edit()
                        .putBoolean(
                            "show_stress_statistics",
                            checked
                        )
                        .apply()
                }
            )
        }

        if (showStressStatistics) {
            StressStatisticsCard(
                days = days,
                start = start,
                end = end
            )
        }
        val selectedWeek = LocalDate.parse(selectedWeekText)
        val weekDays=all.filter{LocalDate.parse(it.date)>=selectedWeek && LocalDate.parse(it.date)<=selectedWeek.plusDays(6)}
        Card(colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text(tr(R.string.ui_weekly_statistics),fontFamily=Garamond,style=MaterialTheme.typography.titleLarge)
                WeekSelector(
                    selectedMonday = selectedWeek,
                    onWeekSelected = {
                        selectedWeekText = it.toString()
                    }
                )
                metricNames.forEachIndexed { m, name ->
                    val values = weekDays.mapNotNull {
                        it.values()[m]
                    }

                    val mean = values
                        .takeIf { it.isNotEmpty() }
                        ?.average()

                    val standardDeviation = recordedVariance(values)?.let {
                        kotlin.math.sqrt(it)
                    }

                    val standardDeviationText = standardDeviation?.let {
                        String.format(AppStrings.locale, "%.2f", it)
                    } ?: "–"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(name)

                            Text(
                                text = tr(R.string.ui_recorded_days_608dc4, values.size),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Ø ${number(mean)}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = tr(R.string.ui_std_dev, standardDeviationText),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Text(
                    text = tr(R.string.ui_standard_deviation_in_points_higher_values_mean_greater),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        val counts = days
            .flatMap { it.selectedFactors() }
            .groupingBy { it }
            .eachCount()

        Text(
            text = tr(R.string.ui_stress_factors),
            fontFamily = Garamond,
            style = MaterialTheme.typography.titleLarge
        )

        PeriodSelector(
            selectedPeriod = period,
            onPeriodSelected = { period = it }
        )

        if (counts.isEmpty()) {
            Text(
                text = tr(R.string.ui_no_stress_factors_have_been_recorded_in_the),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            counts.entries
                .sortedByDescending { it.value }
                .forEach { (label, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f)
                        )

                        Text(tr(R.string.ui_days, count))
                    }
                }
        }
        if (all.isNotEmpty()) {
            OutlinedButton(
                onClick = onOpenEntries,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr(R.string.ui_edit_entries))
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StressStatisticsCard(
    days: List<DayEntry>,
    start: LocalDate,
    end: LocalDate
) {
    val values = days.mapNotNull {
        it.stressIndex()
    }

    val mean = values
        .takeIf { it.isNotEmpty() }
        ?.average()

    val standardDeviation = recordedVariance(values)?.let {
        kotlin.math.sqrt(it)
    }

    val deviationText = standardDeviation?.let {
        String.format(AppStrings.locale, "%.2f", it)
    } ?: "–"

    val dark =
        MaterialTheme.colorScheme.background.luminance() < 0.5f

    val stressColor = if (dark) {
        Color(0xFFC8A2E8)
    } else {
        Color(0xFF7952A3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor =
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = tr(R.string.ui_stress_index),
                fontFamily = Garamond,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = tr(R.string.ui_your_combined_index_of_demands_tension_energy_and),
                style = MaterialTheme.typography.bodySmall
            )

            if (values.isEmpty()) {
                Text(
                    tr(R.string.ui_no_day_in_the_selected_period_has_all)
                )
            } else {
                Text(
                    text = tr(R.string.ui_mean_std_dev, number(mean), deviationText),
                    style = MaterialTheme.typography.titleMedium
                )

                ValuePlot(
                    days = days,
                    start = start,
                    end = end,
                    color = stressColor,
                    valueOf = { day ->
                        day.stressIndex()
                    }
                )

                Text(
                    text = tr(R.string.ui_of_recorded_days_have_complete_values, values.size, days.size),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = tr(R.string.ui_calculation_demands_tension_10_energy_10_room_to),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: Int,
    onPeriodSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            28 to tr(R.string.ui_4_weeks),
            90 to tr(R.string.ui_3_months),
            0 to tr(R.string.ui_all)
        ).forEach { (value, label) ->
            FilterChip(
                selected = selectedPeriod == value,
                onClick = {
                    onPeriodSelected(value)
                },
                label = {
                    Text(label)
                }
            )
        }
    }
}

@Composable
fun EntriesScreen(
    days: List<DayEntry>,
    onEdit: (DayEntry) -> Unit,
    onDeleteWeek: (LocalDate) -> Unit
) {
    var selectedWeekText by rememberSaveable {
        mutableStateOf(monday().toString())
    }

    val selectedWeek = LocalDate.parse(selectedWeekText)
    val weekEnd = selectedWeek.plusDays(6)

    val weekDays = days
        .filter {
            val date = LocalDate.parse(it.date)
            date >= selectedWeek && date <= weekEnd
        }
        .sortedBy { it.date }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = tr(R.string.ui_edit_entries),
                fontFamily = Garamond,
                style = MaterialTheme.typography.headlineLarge
            )
        }

        item {
            WeekSelector(
                selectedMonday = selectedWeek,
                onWeekSelected = {
                    selectedWeekText = it.toString()
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(R.string.ui_recorded_days_608dc4, weekDays.size),
                    modifier = Modifier.weight(1f)
                )

                AppIconButton(
                    icon = Icons.Filled.DeleteOutline,
                    label = tr(R.string.ui_delete_selected_week),
                    onClick = {
                        onDeleteWeek(selectedWeek)
                    },
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (weekDays.isEmpty()) {
            item {
                Text(tr(R.string.ui_no_daily_entries_have_been_recorded_for_this))
            }
        } else {
            item {
                Text(
                    text = tr(R.string.ui_tap_a_day_to_edit_it),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                Text(
                    text = tr(R.string.ui_demands_tension_energy_room_to_act),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(
            count = weekDays.size,
            key = { weekDays[it].date }
        ) { index ->
            val day = weekDays[index]

            OutlinedCard(
                onClick = { onEdit(day) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = LocalDate.parse(day.date).format(
                            DateTimeFormatter.ofPattern(
                                "EEEE, dd.MM.yyyy",
                                AppStrings.locale
                            )
                        ),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = day.values().joinToString(" / ") {
                            number(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ValuePlot(days:List<DayEntry>,start:LocalDate,end:LocalDate,color:Color,valueOf: (DayEntry) -> Double?){
    val axis = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(140.dp)){
        Column(Modifier.width(24.dp).fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween){listOf("10","5","0").forEach{Text(it,style=MaterialTheme.typography.labelSmall)}}
        Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical=6.dp)){
            val span=max(1L,end.toEpochDay()-start.toEpochDay()).toFloat()
            listOf(0f,.5f,1f).forEach{fraction ->drawLine(axis,Offset(0f,size.height*fraction),Offset(size.width,size.height*fraction),1.dp.toPx())}
            var previous:Pair<DayEntry,Offset>?=null
            days.forEach{day ->
                val value=valueOf(day)
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
        Text(start.format(DateTimeFormatter.ofPattern(if (AppStrings.locale.language == "de") "dd.MM." else "MM/dd")),style=MaterialTheme.typography.labelSmall)
        Text(end.format(DateTimeFormatter.ofPattern(if (AppStrings.locale.language == "de") "dd.MM." else "MM/dd")),style=MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun Plot(days: List<DayEntry>, metric: Int, start: LocalDate, end: LocalDate, color: Color) {
    ValuePlot(
        days = days,
        start = start,
        end = end,
        color = color,
        valueOf = { day ->
            day.values()[metric]
        }
    )
}

@Composable
fun Review(model:AppModel,onPhoto:(File)->Unit){
    val scan=model.draft ?: return
    var selected by rememberSaveable{mutableIntStateOf(0)}
    var original by rememberSaveable{mutableStateOf(false)}
    var showWarnings by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text(tr(R.string.ui_recognise_review_save),fontFamily=Garamond,style=MaterialTheme.typography.headlineSmall)
        Text(if(scan.templateId=="MNB-01/left")tr(R.string.ui_onepager_left_page_dot_positions_0_10_green) else tr(R.string.ui_bc_02_page_1_circle_marks_0_10))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=!original,onClick={original=false},label={Text(tr(R.string.ui_recognition))})
            FilterChip(selected=original,onClick={original=true},label={Text(tr(R.string.ui_original))})
        }
        val file=if(original)scan.original else scan.overlay
        LocalPhoto(file,Modifier.fillMaxWidth().height(230.dp).clickable{onPhoto(file)})
        Text(tr(R.string.ui_tap_the_photo_to_enlarge_and_zoom),style=MaterialTheme.typography.labelSmall)
        if (scan.warnings.isNotEmpty()) {
            AppIconButton(
                icon = Icons.Filled.Info,
                label = tr(R.string.scan_warnings_toggle, scan.warnings.size),
                onClick = { showWarnings = !showWarnings }
            )
            if (showWarnings) scan.warnings.forEach {
                Text(
                    DiagnosticLocalizer.display(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        WeekSelector(
            selectedMonday = LocalDate.parse(model.week),
            onWeekSelected = { selectedMonday ->
                model.changeWeek(selectedMonday.toString())
            },
            enabled = !model.busy
        )
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            dayLabels.forEachIndexed{d,label -> FilterChip(selected=selected==d,onClick={selected=d},label={Text(label)})}
        }
        Text(tr(R.string.ui_review_values, dayLabels[selected]),style=MaterialTheme.typography.titleMedium)
        Text(tr(R.string.ui_empty_fields_remain_missing_values_manual_corrections_can),style=MaterialTheme.typography.bodySmall)
        metricNames.forEachIndexed{m,name -> MetricEditor(name,model.editedValues[selected][m],{model.setValue(selected,m,it)})}
        Text(tr(R.string.ui_stress_factors),style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(value=model.editedCustom1,onValueChange={model.editedCustom1=it.take(120)},label={Text(tr(R.string.ui_name_of_extra_factor_1_for_this_week))},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=model.editedCustom2,onValueChange={model.editedCustom2=it.take(120)},label={Text(tr(R.string.ui_name_of_extra_factor_2_for_this_week))},singleLine=true,modifier=Modifier.fillMaxWidth())
        Text(tr(R.string.ui_handwritten_names_are_not_read_please_enter_a),style=MaterialTheme.typography.bodySmall)
        FactorEditor(model.editedFactors[selected],model.customLabels()){model.setFactor(selected,it)}
        Button(onClick={model.saveDraft()},enabled=!model.busy,modifier=Modifier.fillMaxWidth()){Text(tr(R.string.ui_save_week))}
        Text(tr(R.string.ui_only_days_with_at_least_one_value_or),style=MaterialTheme.typography.bodySmall)
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
    if(Integer.bitCount(mask)>3)Text(tr(R.string.ui_more_than_three_factors_selected_please_check),color=MaterialTheme.colorScheme.error)
}

@Composable
fun Settings(model:AppModel){
    val exportZip=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){it?.let{uri->model.export(uri,true)}}
    val exportCsv=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let{uri->model.export(uri,false)}}
    val importZip=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(model::inspectImport)}
    val exportBeforeDelete = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            model.exportBeforeDelete(uri)
        } else {
            // Dateiauswahl abgebrochen: zurück zur ersten Nachfrage.
            model.showDeleteOptions = true
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text(tr(R.string.ui_your_data),fontFamily=Garamond,style=MaterialTheme.typography.headlineLarge)
        Text(tr(R.string.ui_processing_and_storage_take_place_on_this_phone))
        Button(onClick={exportZip.launch("Wochenblick-${LocalDate.now()}.zip")},modifier=Modifier.fillMaxWidth()){Text(tr(R.string.ui_export_full_backup))}
        OutlinedButton(onClick={importZip.launch(arrayOf("application/zip","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text(tr(R.string.ui_import_backup))}
        Text(tr(R.string.ui_zip_includes_values_and_photos_conflicts_are_shown),style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        OutlinedButton(onClick={exportCsv.launch("Wochenblick-${LocalDate.now()}.csv")},modifier=Modifier.fillMaxWidth()){Text(tr(R.string.ui_export_values_as_csv))}
        Text(tr(R.string.ui_for_python_and_excel_utf_8_semicolon_separated),style=MaterialTheme.typography.bodySmall)
        HorizontalDivider()

        Text(
            text = tr(R.string.ui_delete_data),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = tr(R.string.ui_removes_all_values_and_saved_scan_photos_from),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = {
                model.showDeleteOptions = true
            },
            enabled = !model.busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(tr(R.string.ui_delete_all_data))
        }

        HorizontalDivider()

        LanguageSettings(enabled = !model.busy)
        HorizontalDivider()
        ReminderSettings()

        HorizontalDivider()
        Text(tr(R.string.ui_try_it_out),style=MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick=model::testScan,modifier=Modifier.fillMaxWidth()){Text(tr(R.string.ui_analyse_sample_sheet))}
        Text(tr(R.string.ui_opens_a_synthetically_filled_sheet_for_review_sample),style=MaterialTheme.typography.bodySmall)
        Text(tr(R.string.ui_template_onepager_left_page),style=MaterialTheme.typography.titleMedium)
        Text(tr(R.string.ui_the_onepager_template_is_aligned_using_printed_features))
        Text(tr(R.string.ui_version_1_2_1_personal_strain_dashboard),style=MaterialTheme.typography.labelSmall)
    }
    if (model.showDeleteOptions) {
        AlertDialog(
            onDismissRequest = {
                model.showDeleteOptions = false
            },
            title = {
                Text(tr(R.string.ui_create_a_backup_first))
            },
            text = {
                Text(
                    tr(R.string.ui_would_you_like_to_export_your_values_and)
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {
                            model.showDeleteOptions = false

                            exportBeforeDelete.launch(
                                tr(R.string.ui_weekly_review_backup_zip, LocalDate.now())
                            )
                        },
                        enabled = !model.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(R.string.ui_export_backup))
                    }

                    TextButton(
                        onClick = {
                            model.showDeleteOptions = false
                            model.showDeleteConfirmation = true
                        },
                        enabled = !model.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(R.string.ui_continue_without_export))
                    }

                    TextButton(
                        onClick = {
                            model.showDeleteOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(R.string.action_cancel))
                    }
                }
            }
        )
    }

    if (model.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                model.showDeleteConfirmation = false
            },
            title = {
                Text(tr(R.string.ui_really_delete_all_data))
            },
            text = {
                Text(
                    tr(R.string.ui_all_values_and_saved_scan_photos_will_be)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.deleteAllData()
                    },
                    enabled = !model.busy,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(tr(R.string.ui_delete_everything))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        model.showDeleteConfirmation = false
                    }
                ) {
                    Text(tr(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun History(scans:List<ScanEntry>,model:AppModel,onPhoto:(File)->Unit){
    val context=LocalContext.current
    var deleting by remember{mutableStateOf<ScanEntry?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text(tr(R.string.ui_your_scans),fontFamily=Garamond,style=MaterialTheme.typography.headlineLarge)
        if(scans.isEmpty())Text(tr(R.string.ui_no_scans_saved_yet))
        scans.forEach{s -> Card(colors=CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(tr(R.string.ui_week_starting, displayDate(s.weekStart)),style=MaterialTheme.typography.titleMedium)
                Text(s.template,style=MaterialTheme.typography.labelSmall)
                if(s.diagnostics!="[]"){
                    val notes=remember(s.diagnostics, AppStrings.locale){runCatching{
                        val array=org.json.JSONArray(s.diagnostics)
                        (0 until array.length()).joinToString("\n"){DiagnosticLocalizer.display(array.getString(it))}
                    }.getOrDefault(tr(R.string.ui_recognition_notes_could_not_be_read))}
                    Text(tr(R.string.ui_notes_from_this_scan, notes),style=MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    TextButton(onClick={onPhoto(File(context.filesDir,"scans/${s.original}"))}){Text(tr(R.string.ui_original))}
                    TextButton(onClick={onPhoto(File(context.filesDir,"scans/${s.aligned}"))}){Text(tr(R.string.ui_aligned))}
                    TextButton(onClick={deleting=s}){Text(tr(R.string.action_delete))}
                }
            }
        }}
    }
    deleting?.let{s -> AlertDialog(onDismissRequest={deleting=null},title={Text(tr(R.string.ui_delete_scan))},text={Text(tr(R.string.ui_both_photos_will_be_deleted_daily_values_already))},
        confirmButton={TextButton(onClick={model.deleteScan(s);deleting=null}){Text(tr(R.string.action_delete))}},dismissButton={TextButton(onClick={deleting=null}){Text(tr(R.string.action_cancel))}})}
}

@Composable
fun ConflictDialog(title:String,lines:List<String>,onCancel:()->Unit,onKeep:()->Unit,onReplace:()->Unit){
    var replace by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onCancel,title={Text(title)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        lines.forEach{Text(it,style=MaterialTheme.typography.bodySmall)}
        Text(tr(R.string.ui_new_days_will_be_added_for_days_that))
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=replace,onCheckedChange={replace=it});Text(tr(R.string.ui_replace_existing_days_with_incoming_values))}
        if(!replace)Text(tr(R.string.ui_existing_days_will_be_kept_unchanged))
    }},confirmButton={TextButton(onClick={if(replace)onReplace() else onKeep()}){Text(tr(R.string.ui_apply))}},dismissButton={TextButton(onClick=onCancel){Text(tr(R.string.action_cancel))}})
}

@Composable
fun DayEditor(day:DayEntry,onDismiss:()->Unit,onSave:(DayEntry)->Unit,onDelete:()->Unit){
    var values by remember(day){mutableStateOf(day.values())}
    var factors by remember(day){mutableIntStateOf(day.factors)}
    var custom1 by remember(day){mutableStateOf(day.custom1)}
    var custom2 by remember(day){mutableStateOf(day.custom2)}
    var delete by remember{mutableStateOf(false)}
    if(delete){AlertDialog(onDismissRequest={delete=false},title={Text(tr(R.string.ui_delete_day))},text={Text(tr(R.string.ui_values_for_will_be_deleted_the_scan_photo, displayDate(day.date)))},
        confirmButton={TextButton(onClick=onDelete){Text(tr(R.string.action_delete))}},dismissButton={TextButton(onClick={delete=false}){Text(tr(R.string.action_cancel))}});return}
    AlertDialog(onDismissRequest=onDismiss,title={Text(displayDate(day.date))},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())){
        metricNames.forEachIndexed{m,name -> MetricEditor(name,values[m]){v->values=values.mapIndexed{i,old->if(i==m)v else old}}}
        OutlinedTextField(value=custom1,onValueChange={custom1=it.take(120)},label={Text(tr(R.string.ui_extra_factor_1_for_this_entry))},isError=factors and 256!=0 && custom1.isBlank(),singleLine=true)
        OutlinedTextField(value=custom2,onValueChange={custom2=it.take(120)},label={Text(tr(R.string.ui_extra_factor_2_for_this_entry))},isError=factors and 512!=0 && custom2.isBlank(),singleLine=true)
        FactorEditor(factors,factorNames.take(8)+listOf(custom1.ifBlank{factorNames[8]},custom2.ifBlank{factorNames[9]})){f->factors=factors xor (1 shl f)}
        TextButton(onClick={delete=true}){Text(tr(R.string.ui_delete_day_c8db52))}
    }},confirmButton={TextButton(enabled=(factors and 256==0 || custom1.isNotBlank()) && (factors and 512==0 || custom2.isNotBlank()),onClick={onSave(day.copy(demands=values[0],tension=values[1],energy=values[2],agency=values[3],factors=factors,custom1=custom1.trim(),custom2=custom2.trim()))}){Text(tr(R.string.action_save))}},dismissButton={TextButton(onClick=onDismiss){Text(tr(R.string.action_cancel))}})
}

@Composable
fun LocalPhoto(file:File,modifier:Modifier=Modifier){
    var failed by remember(file.path){mutableStateOf(false)}
    val bitmap by produceState<android.graphics.Bitmap?>(null,file.path){
        value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){runCatching{loadOriented(file)}.getOrNull()}
        failed=value==null
    }
    bitmap?.let{Image(it.asImageBitmap(),contentDescription=tr(R.string.ui_worksheet),modifier=modifier,contentScale=ContentScale.Fit)} ?: Box(modifier,contentAlignment=Alignment.Center){Text(if(failed)tr(R.string.ui_could_not_load_the_photo) else tr(R.string.ui_loading_image))}
}
@Composable
fun PhotoDialog(file:File,onClose:()->Unit){
    var zoom by remember{mutableFloatStateOf(1f)}
    var offset by remember{mutableStateOf(Offset.Zero)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                TextButton(onClick={zoom=1f;offset=Offset.Zero}){Text(tr(R.string.ui_reset_view))}
                TextButton(onClick=onClose){Text(tr(R.string.action_close))}
            }
            Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit){detectTransformGestures{_,pan,scale,_->
                zoom=(zoom*scale).coerceIn(1f,6f);offset=if(zoom<=1f)Offset.Zero else offset+pan
            }},contentAlignment=Alignment.Center){
                LocalPhoto(file,Modifier.fillMaxSize().graphicsLayer{scaleX=zoom;scaleY=zoom;translationX=offset.x;translationY=offset.y})
            }
            Text(tr(R.string.ui_use_two_fingers_to_zoom_and_pan),style=MaterialTheme.typography.labelSmall)
        }
    }
}
