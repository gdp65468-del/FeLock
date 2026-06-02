package com.selflock.app.data.local.converter

import androidx.room.TypeConverter
import com.selflock.app.domain.model.BlockReason
import com.selflock.app.domain.model.BlockType
import com.selflock.app.domain.model.TargetType

class Converters {
    @TypeConverter fun fromBlockType(value: BlockType): String = value.name
    @TypeConverter fun toBlockType(value: String): BlockType = BlockType.valueOf(value)
    @TypeConverter fun fromTargetType(value: TargetType): String = value.name
    @TypeConverter fun toTargetType(value: String): TargetType = TargetType.valueOf(value)
    @TypeConverter fun fromBlockReason(value: BlockReason): String = value.name
    @TypeConverter fun toBlockReason(value: String): BlockReason = BlockReason.valueOf(value)
}
