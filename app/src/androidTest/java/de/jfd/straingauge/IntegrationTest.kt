package de.jfd.straingauge

import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Runs on a real Android runtime: OpenCV JNI + Room + file streams. */
class IntegrationTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun sample()=File.createTempFile("integration-",".jpg",context.cacheDir).also{f ->
        context.assets.open("sample_scan.jpg").use{input -> f.outputStream().use{input.copyTo(it)}}
    }
    @Test fun nativeScannerReadsPerspectiveAndFactors(){
        val input=sample();val result=SheetScanner(context).analyze(input)
        try {
            val expected=JSONObject(context.assets.open("sample_expected.json").bufferedReader().use{it.readText()})
            for(m in 0..3)for(d in 0..6)assertEquals(expected.getJSONArray("values").getJSONArray(m).getDouble(d),result.values[m][d]!!,.001)
            for(d in 0..6)assertEquals(expected.getJSONArray("factors").getInt(d),result.factors[d])
            assertTrue(result.warnings.toString(),result.warnings.isEmpty())
        }finally{input.delete();result.aligned.delete();result.overlay.delete()}
    }
    @Test fun backupRoundTripIsLosslessAndRepeatedImportDoesNotDuplicateScans()=runBlocking{
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val target=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val directory=File(context.filesDir,"scans").apply{mkdirs()}
        val id=UUID.randomUUID().toString()
        val original=File(directory,"$id-original.jpg");val aligned=File(directory,"$id-aligned.jpg")
        val zip=File.createTempFile("backup-test-",".zip",context.cacheDir)
        try {
            context.assets.open("sample_scan.jpg").use{input -> original.outputStream().use{input.copyTo(it)}}
            original.copyTo(aligned)
            val scan=ScanEntry(id,"2026-10-12",original.name,aligned.name)
            val day=DayEntry("2026-10-12",0.0,null,7.3,10.0,261,id,"[0,null,7,10]",1234,custom1="Unplanable Schedule")
            db.store().putScans(listOf(scan));db.store().putDays(listOf(day))
            val exporter=BackupManager(context,db);val importer=BackupManager(context,target)
            exporter.exportZip(Uri.fromFile(zip))
            // Remove source photo files to model a fresh install in the same test context.
            original.delete();aligned.delete()
            importer.importArchive(importer.readZip(Uri.fromFile(zip)),false)
            assertEquals(listOf(day),target.store().days())
            assertEquals(listOf(scan),target.store().scans())
            importer.importArchive(importer.readZip(Uri.fromFile(zip)),false)
            assertEquals(1,target.store().days().size)
            assertEquals(1,target.store().scans().size)
        }finally{db.close();target.close();original.delete();aligned.delete();zip.delete()}
    }
    @Test fun archiveCannotEscapeStagingDirectory(){
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        val zip=File.createTempFile("bad-backup-",".zip",context.cacheDir)
        try{
            ZipOutputStream(zip.outputStream()).use{it.putNextEntry(ZipEntry("../escape.jpg"));it.write(byteArrayOf(1));it.closeEntry()}
            try { BackupManager(context,db).readZip(Uri.fromFile(zip));fail("Unsafe path accepted") }
            catch(expected:IllegalArgumentException){assertTrue(expected.message!!.contains("Archivpfad"))}
        }finally{db.close();zip.delete()}
    }
    @Test fun onePagerNativeScannerReadsContinuousValuesAndExtraRows(){
        val input=File.createTempFile("onepager-test-",".jpg",context.cacheDir)
        context.assets.open("onepager_sample.jpg").use{source->input.outputStream().use{source.copyTo(it)}}
        val result=SheetScanner(context).analyze(input)
        try {
            assertEquals("MNB-01/left",result.templateId)
            val expected=JSONObject(context.assets.open("onepager_sample_expected.json").bufferedReader().use{it.readText()})
            for(m in 0..3)for(d in 0..6)assertEquals(expected.getJSONArray("values").getJSONArray(m).getDouble(d),result.values[m][d]!!,.22)
            for(d in 0..6)assertEquals(expected.getJSONArray("factors").getInt(d),result.factors[d])
        }finally{input.delete();result.aligned.delete();result.overlay.delete()}
    }
    @Test fun migrationOneToTwoKeepsExistingDays()=runBlocking{
        val name="migration-check.db"
        context.deleteDatabase(name)
        val file=context.getDatabasePath(name);file.parentFile?.mkdirs()
        val sqlite=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file,null)
        sqlite.execSQL("CREATE TABLE days (date TEXT NOT NULL PRIMARY KEY, demands REAL, tension REAL, energy REAL, agency REAL, factors INTEGER NOT NULL, scanId TEXT, detected TEXT, updatedAt INTEGER NOT NULL)")
        sqlite.execSQL("CREATE TABLE scans (id TEXT NOT NULL PRIMARY KEY, weekStart TEXT NOT NULL, original TEXT NOT NULL, aligned TEXT NOT NULL, template TEXT NOT NULL, createdAt INTEGER NOT NULL, diagnostics TEXT NOT NULL)")
        sqlite.execSQL("INSERT INTO days VALUES ('2026-10-12',0,NULL,7.3,10,5,NULL,NULL,1234)")
        sqlite.version=1;sqlite.close()
        val db=Room.databaseBuilder(context,AppDatabase::class.java,name).addMigrations(MIGRATION_1_2).build()
        try {
            val day=db.store().days().single()
            assertEquals(0.0,day.demands!!,0.0);assertNull(day.tension)
            assertEquals(7.3,day.energy!!,0.0);assertEquals("",day.custom1);assertEquals(5,day.factors)
        }finally{db.close();context.deleteDatabase(name)}
    }
}
