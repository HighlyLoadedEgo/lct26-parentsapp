package ru.nksk.parentsapp.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigatorTest {
    private data object Home : NavKey
    private data class Detail(val id: String) : NavKey

    @Test
    fun twoDifferentCallbacksOnlyOpenTheFirstDestination() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)
        val openFirst = { navigator.navigate(source = Home, destination = Detail("first")) }
        val openSecond = { navigator.navigate(source = Home, destination = Detail("second")) }

        openFirst()
        openSecond()

        assertEquals(listOf<NavKey>(Home, Detail("first")), stack)
    }

    @Test
    fun repeatedTapsDoNotDuplicateTheCurrentDestination() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)

        navigator.navigate(source = Home, destination = Detail("first"))
        navigator.navigate(source = Home, destination = Detail("first"))
        navigator.goBack(source = Detail("first"))

        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun navigatingToTheCurrentDestinationKeepsASingleEntry() {
        val stack = mutableListOf<NavKey>(Home, Detail("first"))
        val navigator = AppNavigator(stack)

        navigator.navigate(source = Detail("first"), destination = Detail("first"))

        assertEquals(listOf<NavKey>(Home, Detail("first")), stack)
    }

    @Test
    fun differentArgumentsCreateDistinctEntriesAndBackRestoresThePreviousOne() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)

        navigator.navigate(source = Home, destination = Detail("first"))
        navigator.navigate(source = Detail("first"), destination = Detail("second"))
        assertEquals(listOf<NavKey>(Home, Detail("first"), Detail("second")), stack)

        navigator.goBack(source = Detail("second"))
        assertEquals(listOf<NavKey>(Home, Detail("first")), stack)
    }

    @Test
    fun staleFeatureBackCannotPopANewerDestination() {
        val stack = mutableListOf<NavKey>(Home, Detail("first"))
        val navigator = AppNavigator(stack)
        val backFromFirst = { navigator.goBack(source = Detail("first")) }

        navigator.navigate(source = Detail("first"), destination = Detail("second"))
        backFromFirst()

        assertEquals(listOf<NavKey>(Home, Detail("first"), Detail("second")), stack)
    }

    @Test
    fun repeatedFeatureBackOnlyRemovesItsOwnEntry() {
        val stack = mutableListOf<NavKey>(Home, Detail("first"), Detail("second"))
        val navigator = AppNavigator(stack)
        val backFromSecond = { navigator.goBack(source = Detail("second")) }

        backFromSecond()
        backFromSecond()

        assertEquals(listOf<NavKey>(Home, Detail("first")), stack)
    }

    @Test
    fun featureBackAtRootNeverRemovesTheStartDestination() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)

        navigator.goBack(source = Home)

        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun systemBackRemovesTheCurrentDestination() {
        val stack = mutableListOf<NavKey>(Home, Detail("first"), Detail("second"))
        val navigator = AppNavigator(stack)

        navigator.goBack()

        assertEquals(listOf<NavKey>(Home, Detail("first")), stack)
    }

    @Test
    fun systemBackAtRootNeverEmptiesTheStack() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)

        repeat(3) { navigator.goBack() }

        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun replaceSwapsOnlyTheCurrentEntry() {
        val proposal = Detail("scanner")
        val stack = mutableListOf<NavKey>(Home, proposal)
        val navigator = AppNavigator(stack)

        navigator.replace(proposal, Detail("statistics"))
        navigator.replace(proposal, Detail("duplicate"))
        navigator.replace(Home, Detail("cannot-replace-non-current"))

        assertEquals(listOf<NavKey>(Home, Detail("statistics")), stack)
    }

    @Test
    fun replaceCannotSwapTheOnlyStackEntry() {
        val stack = mutableListOf<NavKey>(Home)
        val navigator = AppNavigator(stack)

        navigator.replace(Home, Detail("replacement"))

        assertEquals(listOf<NavKey>(Home), stack)
    }

    @Test
    fun resetToReplacesTheWholeStackWithOneDestination() {
        val stack = mutableListOf<NavKey>(Home, Detail("pin_setup"), Detail("pin_lock"))
        val navigator = AppNavigator(stack)

        navigator.resetTo(Detail("statistics"))

        assertEquals(listOf<NavKey>(Detail("statistics")), stack)
    }

    @Test
    fun resetToTheCurrentRootIsANoOp() {
        val statistics = Detail("statistics")
        val stack = mutableListOf<NavKey>(statistics)
        val navigator = AppNavigator(stack)

        navigator.resetTo(statistics)

        assertEquals(listOf<NavKey>(statistics), stack)
    }
}
