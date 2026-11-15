package com.webcraft.ide.data.db

import androidx.room.TypeConverter
import java.util.Date

object WebCraftConverters {
    @TypeConverter @JvmStatic
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter @JvmStatic
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
