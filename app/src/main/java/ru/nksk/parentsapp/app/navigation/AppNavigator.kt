package ru.nksk.parentsapp.app.navigation

import androidx.navigation3.runtime.NavKey

/** Navigation policy, independent of UI and of the storage used for the back stack. */
internal class AppNavigator(private val backStack: MutableList<NavKey>) {
    fun navigate(source: NavKey, destination: NavKey) {
        // Check the stack immediately; the outgoing entry may still be resumed.
        if (backStack.lastOrNull() != source) return

        // Single-top navigation: repeated taps must not add the same screen twice.
        if (source != destination) {
            backStack.add(destination)
        }
    }

    fun goBack(source: NavKey) {
        if (backStack.lastOrNull() == source) {
            goBack()
        }
    }

    /** Swaps the current entry in place; used when one screen hands off to its successor. */
    fun replace(source: NavKey, destination: NavKey) {
        if (backStack.lastOrNull() == source && backStack.size > 1) {
            backStack[backStack.lastIndex] = destination
        }
    }

    /** Replaces the whole stack; used by the PIN gates and the bottom-bar tabs to swap the stack without Back history. */
    fun resetTo(destination: NavKey) {
        if (backStack.lastOrNull() == destination && backStack.size == 1) return
        backStack.clear()
        backStack.add(destination)
    }

    /** System Back targets the current stack, independently of an entry callback. */
    fun goBack() {
        // NavDisplay lets the activity handle Back at the root; never empty its stack.
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }
}
