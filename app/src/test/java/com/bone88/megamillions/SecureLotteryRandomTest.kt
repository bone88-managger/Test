package com.bone88.megamillions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class SecureLotteryRandomTest {
    @Test
    fun historicalColumnAnalysisSortsDrawsChronologicallyAndFindsLatest() {
        val older = Draw("2026-01-01", listOf(5, 15, 25, 35, 45), 1)
        val middle = Draw("2026-01-08", listOf(8, 15, 26, 35, 44), 2)
        val newest = Draw("2026-01-15", listOf(6, 17, 26, 36, 44), 3)

        val result = analyzeOfficialDrawHistory(listOf(newest, older, middle))

        assertEquals(newest, result.latestDraw)
        assertEquals(2, result.transitions)
        assertEquals(1, result.columns[0].greater)
        assertEquals(1, result.columns[0].lower)
        assertEquals(0, result.columns[0].equal)
        assertEquals(mapOf(-2 to 1, 3 to 1), result.columns[0].intervals)
        assertEquals("Mayor", result.columns[2].leadingDirection)
    }

    @Test
    fun historicalColumnAnalysisHandlesSingleDrawWithoutInventingTransitions() {
        val only = Draw("2026-01-01", listOf(1, 2, 3, 4, 5), 6)

        val result = analyzeOfficialDrawHistory(listOf(only))

        assertEquals(only, result.latestDraw)
        assertEquals(0, result.transitions)
        assertTrue(result.columns.all { it.total == 0 && it.intervals.isEmpty() })
    }

    @Test
    fun historicalColumnAnalysisKeepsColumnsIndependent() {
        val first = Draw("2026-01-01", listOf(1, 10, 20, 30, 40), 1)
        val second = Draw("2026-01-02", listOf(2, 9, 22, 27, 45), 2)

        val result = analyzeOfficialDrawHistory(listOf(first, second))

        assertEquals(1, result.columns[0].greater)
        assertEquals(1, result.columns[1].lower)
        assertEquals(1, result.columns[2].greater)
        assertEquals(1, result.columns[3].lower)
        assertEquals(1, result.columns[4].greater)
    }

    @Test
    fun historicalColumnRangesUseMinimumAndMaximumFromEveryImportedDraw() {
        val first = Draw("2026-01-01", listOf(3, 12, 24, 35, 46), 1)
        val middle = Draw("2026-01-08", listOf(1, 18, 21, 39, 52), 2)
        val newest = Draw("2026-01-15", listOf(25, 14, 30, 31, 70), 3)

        val ranges = analyzeHistoricalColumnRanges(listOf(newest, first, middle))

        assertEquals(
            listOf(
                HistoricalColumnRange(1, 1, 25),
                HistoricalColumnRange(2, 12, 18),
                HistoricalColumnRange(3, 21, 30),
                HistoricalColumnRange(4, 31, 39),
                HistoricalColumnRange(5, 46, 70)
            ),
            ranges
        )
    }

    @Test
    fun historicalColumnRangesReportNoDataForEmptyHistory() {
        val ranges = analyzeHistoricalColumnRanges(emptyList())

        assertEquals(5, ranges.size)
        assertTrue(ranges.all { it.minimum == null && it.maximum == null })
    }

    @Test
    fun individualFirstColumnAnalysisCountsAllOccurrencesAndUsesRequestedDirections() {
        val older = Draw("2026-01-01", listOf(7, 20, 30, 40, 50), 1)
        val middle = Draw("2026-01-08", listOf(5, 21, 31, 41, 51), 2)
        val newest = Draw("2026-01-15", listOf(7, 22, 32, 42, 52), 3)
        val latest = Draw("2026-01-22", listOf(9, 23, 33, 43, 53), 4)

        // Input order is deliberately shuffled. Reference 7 appears in two draws.
        val result = analyzeFirstColumnNeighbors(listOf(latest, middle, older, newest), 7)

        assertEquals(2, result.occurrences)
        assertEquals(listOf(5 to 1, 9 to 1), result.above.top)
        assertEquals(listOf(20 to 1, 22 to 1), result.right.top)
        assertEquals(listOf(5 to 1), result.below.top)
        assertEquals(2, result.above.samples)
        assertEquals(2, result.right.samples)
        assertEquals(1, result.below.samples)
    }

    @Test
    fun individualFirstColumnAnalysisReturnsUpToEightNumbersAndBreaksTiesByValue() {
        val draws = mutableListOf<Draw>()
        for (index in 0 until 8) {
            val day = index * 2 + 1
            draws += Draw("2026-02-" + day.toString().padStart(2, '0'), listOf(7, 20 + index, 30 + index, 40 + index, 50 + index), 1)
            draws += Draw("2026-02-" + (day + 1).toString().padStart(2, '0'), listOf(11 + index, 21 + index, 31 + index, 41 + index, 51 + index), 2)
        }

        val result = analyzeFirstColumnNeighbors(draws, 7)

        assertEquals(8, result.above.top.size)
        assertEquals((11..18).map { it to 1 }, result.above.top)
        assertEquals((20..27).map { it to 1 }, result.right.top)
        assertEquals((11..17).map { it to 1 }, result.below.top)
    }

    @Test
    fun importedDrawsMustUseOfficialMegaMillionsRanges() {
        assertTrue(isValidDrawNumbers(listOf(1, 2, 3, 4, 70), 24))
        assertFalse(isValidDrawNumbers(listOf(1, 2, 3, 4, 71), 12))
        assertFalse(isValidDrawNumbers(listOf(1, 2, 3, 4, 5), 25))
        assertFalse(isValidDrawNumbers(listOf(1, 2, 3, 4, 4), 12))
        assertFalse(isValidDrawNumbers(listOf(1, 2, 3, 4), 12))
    }
    @Test
    fun duplicateComparisonUsesOnlyFiveWhiteBallsAndIgnoresMegaBall() {
        val candidate = Draw("candidate", listOf(10, 20, 30, 40, 50), 1)
        val sameWhitesDifferentMega = listOf(Draw("past", listOf(10, 20, 30, 40, 50), 24))
        assertTrue(hasWhiteBallDuplicate(candidate, sameWhitesDifferentMega))
    }

    @Test
    fun duplicateComparisonRequiresTheSameWhiteBallPositions() {
        val candidate = Draw("candidate", listOf(10, 20, 30, 40, 50), 1)
        val differentWhites = listOf(Draw("past", listOf(10, 20, 30, 40, 51), 1))
        assertFalse(hasWhiteBallDuplicate(candidate, differentWhites))
    }

    @Test
    fun historicalFilterAllowsExactlyOneWhiteBallMatch() {
        val candidate = Draw("candidate", listOf(1, 2, 3, 4, 5), 12)
        val history = listOf(Draw("past", listOf(1, 9, 6, 7, 8), 20))
        assertFalse(hasHistoricalWhiteConflict(candidate, history))
    }

    @Test
    fun historicalFilterRejectsExactlyTwoWhiteBallMatches() {
        val candidate = Draw("candidate", listOf(1, 2, 3, 4, 5), 12)
        val history = listOf(Draw("past", listOf(1, 2, 6, 7, 8), 20))
        assertTrue(hasHistoricalWhiteConflict(candidate, history))
    }

    @Test
    fun historicalFilterRejectsThreeWhiteBallMatches() {
        val candidate = Draw("candidate", listOf(1, 2, 3, 4, 5), 12)
        val history = listOf(Draw("past", listOf(1, 2, 3, 6, 7), 20))
        assertTrue(hasHistoricalWhiteConflict(candidate, history))
    }

    @Test
    fun megaBallValuesNeverCountAsHistoricalMatches() {
        // Candidate Mega Ball equals a historical white ball; historical Mega Ball
        // equals a candidate white ball. Neither cross-match may count.
        val candidate = Draw("candidate", listOf(1, 2, 3, 4, 5), 12)
        val history = listOf(Draw("past", listOf(12, 13, 14, 15, 16), 1))
        assertFalse(hasHistoricalWhiteConflict(candidate, history))
    }


    @Test
    fun historicalFilterAllowsThreeSharedNumbersWhenTheyAreInDifferentColumns() {
        val candidate = Draw("candidate", listOf(10, 20, 26, 35, 45), 12)
        val history = listOf(Draw("past", listOf(10, 26, 32, 45, 50), 20))
        // Shared values are 10, 26 and 45, but only 10 is in the same column.
        assertFalse(hasHistoricalWhiteConflict(candidate, history))
    }

    @Test
    fun columnFilterRejectsThreeMatchesInTheSamePositions() {
        val candidate = Draw("candidate", listOf(10, 26, 32, 45, 50), 12)
        val previous = Draw("previous", listOf(10, 26, 35, 45, 60), 20)
        assertTrue(hasWhiteColumnConflict(candidate, previous))
    }

    @Test
    fun columnFilterAllowsOnlyOneMatchInTheSamePositions() {
        val candidate = Draw("candidate", listOf(10, 26, 32, 45, 50), 12)
        val previous = Draw("previous", listOf(10, 20, 35, 40, 60), 20)
        assertFalse(hasWhiteColumnConflict(candidate, previous))
    }

    @Test
    fun columnFilterRejectsTwoMatchesInTheSamePositions() {
        val candidate = Draw("candidate", listOf(10, 26, 32, 45, 50), 12)
        val previous = Draw("previous", listOf(10, 26, 35, 40, 60), 20)
        assertTrue(hasWhiteColumnConflict(candidate, previous))
    }

    @Test
    fun boundedSamplerRejectsIncompleteTail() {
        // For bound 3, 2^32 - 1 is the first value outside the divisible range.
        assertNull(SecureLotteryRandom.mapUnsignedToBound(4_294_967_295L, 3))
        assertEquals(2, SecureLotteryRandom.mapUnsignedToBound(4_294_967_294L, 3))
        assertEquals(0, SecureLotteryRandom.mapUnsignedToBound(0L, 3))
    }

    @Test
    fun whiteBallsAreDistinctAndWithinCurrentRange() {
        repeat(2_000) {
            val white = SecureLotteryRandom.sampleWhite()
            assertEquals(5, white.size)
            assertEquals(5, white.distinct().size)
            assertEquals(white.sorted(), white)
            assertTrue(white.all { it in 1..70 })
        }
    }

    @Test
    fun megaBallStaysWithinItsIndependentRange() {
        repeat(5_000) {
            val mega = SecureLotteryRandom.sampleMega()
            assertTrue(mega in 1..24)
        }
    }

    @Test
    fun whiteNumberDistributionPassesBroadChiSquareCheck() {
        val counts = IntArray(70)
        val random = SecureRandom()
        repeat(20_000) {
            SecureLotteryRandom.sampleWhite(random).forEach { counts[it - 1]++ }
        }
        val expected = counts.sum().toDouble() / counts.size
        val chiSquare = counts.sumOf { count ->
            val difference = count - expected
            difference * difference / expected
        }
        assertTrue("Unexpected white-ball frequency imbalance: " + chiSquare, chiSquare < 120.0)
    }

    @Test
    fun megaBallDistributionPassesBroadChiSquareCheck() {
        val counts = IntArray(24)
        val random = SecureRandom()
        repeat(60_000) {
            counts[SecureLotteryRandom.sampleMega(random) - 1]++
        }
        val expected = counts.sum().toDouble() / counts.size
        val chiSquare = counts.sumOf { count ->
            val difference = count - expected
            difference * difference / expected
        }
        assertTrue("Unexpected Mega Ball frequency imbalance: " + chiSquare, chiSquare < 60.0)
    }

    @Test
    fun numberRangesDoNotReceiveAnArtificialPreference() {
        val counts = IntArray(3)
        val random = SecureRandom()
        repeat(20_000) {
            SecureLotteryRandom.sampleWhite(random).forEach { number ->
                when (number) {
                    in 1..23 -> counts[0]++
                    in 24..46 -> counts[1]++
                    in 47..70 -> counts[2]++
                }
            }
        }
        val total = counts.sum().toDouble()
        val expected = doubleArrayOf(total * 23 / 70, total * 23 / 70, total * 24 / 70)
        val chiSquare = counts.indices.sumOf { index ->
            val difference = counts[index] - expected[index]
            difference * difference / expected[index]
        }
        assertTrue("Unexpected range preference: " + chiSquare, chiSquare < 20.0)
        assertNotNull(counts)
        assertFalse(counts.all { it == 0 })
    }
}
