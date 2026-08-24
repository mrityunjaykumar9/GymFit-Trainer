package com.freegymtrainer.app.data.local

import androidx.room.TypeConverter

/** Lists are stored as ";;"-delimited strings - avoids pulling in a JSON library for the MVP. */
class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(";;")

    @TypeConverter
    fun toStringList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(";;")
}
