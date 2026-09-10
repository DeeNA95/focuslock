package com.focuslock.testutil

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.focuslock.data.db.FocusLockDatabase

fun buildInMemoryDatabase(): FocusLockDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        FocusLockDatabase::class.java,
    ).allowMainThreadQueries().build()
