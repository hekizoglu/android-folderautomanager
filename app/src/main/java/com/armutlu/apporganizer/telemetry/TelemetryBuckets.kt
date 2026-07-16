package com.armutlu.apporganizer.telemetry

enum class CountBucket(val wireValue: String) {
    ZERO("0"), ONE_TO_FIVE("1_5"), SIX_TO_TEN("6_10"),
    ELEVEN_TO_TWENTY("11_20"), TWENTY_ONE_TO_FIFTY("21_50"),
    FIFTY_ONE_TO_HUNDRED("51_100"), ONE_HUNDRED_ONE_PLUS("101_plus");

    companion object {
        fun from(value: Int): CountBucket = when (value.coerceAtLeast(0)) {
            0 -> ZERO
            in 1..5 -> ONE_TO_FIVE
            in 6..10 -> SIX_TO_TEN
            in 11..20 -> ELEVEN_TO_TWENTY
            in 21..50 -> TWENTY_ONE_TO_FIFTY
            in 51..100 -> FIFTY_ONE_TO_HUNDRED
            else -> ONE_HUNDRED_ONE_PLUS
        }
    }
}

enum class QueryLengthBucket(val wireValue: String) {
    ZERO("0"), ONE_TO_THREE("1_3"), FOUR_TO_EIGHT("4_8"), NINE_PLUS("9_plus");

    companion object {
        fun from(value: Int): QueryLengthBucket = when (value.coerceAtLeast(0)) {
            0 -> ZERO
            in 1..3 -> ONE_TO_THREE
            in 4..8 -> FOUR_TO_EIGHT
            else -> NINE_PLUS
        }
    }
}

enum class FolderAppCountBucket(val wireValue: String) {
    ONE_TO_FIVE("1_5"), SIX_TO_TEN("6_10"), ELEVEN_TO_TWENTY("11_20"), TWENTY_ONE_PLUS("21_plus");

    companion object {
        fun from(value: Int): FolderAppCountBucket = when (value.coerceAtLeast(1)) {
            in 1..5 -> ONE_TO_FIVE
            in 6..10 -> SIX_TO_TEN
            in 11..20 -> ELEVEN_TO_TWENTY
            else -> TWENTY_ONE_PLUS
        }
    }
}
