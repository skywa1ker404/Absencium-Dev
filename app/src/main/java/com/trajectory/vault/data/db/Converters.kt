package com.trajectory.vault.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/** Dates and times are stored as ISO-8601 text. Enums are handled by Room itself. */
class Converters {

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }
}
