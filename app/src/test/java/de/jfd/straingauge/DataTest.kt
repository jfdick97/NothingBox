package de.jfd.straingauge
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
import org.json.JSONObject

class DataTest {
    private fun day(date:String="2026-10-12",energy:Double?=null)=DayEntry(date,0.0,4.0,energy,8.0)
    @Test fun missingAndZeroAreDistinct(){
        val input=day()
        val decoded=BackupJson.parseDays(JSONArray().put(BackupJson.day(input))).single()
        assertEquals(0.0,decoded.demands!!,0.0);assertNull(decoded.energy)
    }
    @Test fun roundTripPreservesCorrectionsAndOriginal(){
        val input=day(energy=6.4).copy(detected="[0,4,6,8]",scanId="scan-one",factors=5)
        assertEquals(input,BackupJson.parseDays(JSONArray().put(BackupJson.day(input))).single())
    }
    @Test fun duplicateDateRequiresPolicy(){
        val old=listOf(day(energy=3.0));val incoming=listOf(day(energy=7.0),day("2026-10-13",2.0))
        assertEquals(listOf("2026-10-12"),Entries.conflicts(old,incoming))
        assertEquals(1,Entries.merge(old,incoming,false).size)
        assertEquals(2,Entries.merge(old,incoming,true).size)
    }
    @Test(expected=IllegalArgumentException::class) fun duplicateIncomingRejected(){Entries.merge(emptyList(),listOf(day(),day()),false)}
    @Test(expected=IllegalArgumentException::class) fun outsideScaleRejected(){day(energy=11.0).validate()}
    @Test(expected=IllegalArgumentException::class) fun nanRejected(){day(energy=Double.NaN).validate()}
    @Test(expected=IllegalArgumentException::class) fun invalidFactorsRejected(){day().copy(factors=1024).validate()}
    @Test(expected=IllegalArgumentException::class) fun invalidShapeRejected(){
        val j=BackupJson.day(day()).put("values",JSONArray().put(2))
        BackupJson.parseDays(JSONArray().put(j))
    }
    @Test fun extraFactorsAndNamesRoundTrip(){
        val input=day(energy=6.35).copy(factors=768,custom1="Unplanable Schedule",custom2="Meetings")
        input.validate()
        assertEquals(input,BackupJson.parseDays(JSONArray().put(BackupJson.day(input))).single())
        assertEquals(listOf("Unplanable Schedule","Meetings"),input.selectedFactors())
    }
    @Test fun legacyJsonKeepsNullAndNeedsNoNewNames(){
        val payload=BackupJson.day(day()).apply{remove("custom1");remove("custom2")}
        val result=BackupJson.parseDays(JSONArray().put(payload)).single()
        assertEquals("",result.custom1);assertNull(result.energy)
    }
    @Test(expected=IllegalArgumentException::class) fun selectedExtraFactorNeedsName(){day().copy(factors=256).validate()}
    @Test(expected=IllegalArgumentException::class) fun excessivelyLongLabelRejected(){day().copy(custom1="x".repeat(121)).validate()}
    @Test fun renamedFactorCreatesConflict(){
        val old=day().copy(factors=256,custom1="One")
        val incoming=old.copy(custom1="Two")
        assertEquals(listOf(old.date),Entries.conflicts(listOf(old),listOf(incoming)))
    }
}
