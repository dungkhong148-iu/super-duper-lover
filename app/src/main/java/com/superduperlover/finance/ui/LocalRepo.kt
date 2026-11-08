package com.superduperlover.finance.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.superduperlover.finance.data.Repo

/** Screens read and write storage only through this. Provided once in MainActivity. */
val LocalRepo = staticCompositionLocalOf<Repo> { error("Repo not provided") }
