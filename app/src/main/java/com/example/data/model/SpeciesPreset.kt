package com.example.data.model

data class SpeciesPreset(
    val id: String,
    val name: String,
    val commonName: String,
    val incubationDays: Int,
    val lockdownDay: Int,
    val targetTempC: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val targetHumidityPct: Double,
    val minHumidityPct: Double,
    val maxHumidityPct: Double,
    val lockdownHumidityPct: Double,
    val recommendedTurnsPerDay: Int,
    val candlingDays: List<Int>,
    val requiresTurning: Boolean = true,
    val tips: String
) {
    val targetTempF: Double
        get() = (targetTempC * 9.0 / 5.0) + 32.0

    companion object {
        val ALL: List<SpeciesPreset> = listOf(
            SpeciesPreset(
                id = "chicken",
                name = "Chicken (Gallus gallus)",
                commonName = "Chicken",
                incubationDays = 21,
                lockdownDay = 18,
                targetTempC = 37.5,
                minTempC = 37.2,
                maxTempC = 38.3,
                targetHumidityPct = 50.0,
                minHumidityPct = 45.0,
                maxHumidityPct = 55.0,
                lockdownHumidityPct = 65.0,
                recommendedTurnsPerDay = 5,
                candlingDays = listOf(7, 14, 18),
                requiresTurning = true,
                tips = "Turn eggs at least 3-5 times daily (or every 2-3 hours) with sharp end pointed down. On Day 18 (Lockdown), stop all egg turning and increase humidity to 65% to soften egg membranes for pipping."
            ),
            SpeciesPreset(
                id = "duck",
                name = "Duck (Anas platyrhynchos)",
                commonName = "Duck",
                incubationDays = 28,
                lockdownDay = 25,
                targetTempC = 37.5,
                minTempC = 37.2,
                maxTempC = 38.2,
                targetHumidityPct = 55.0,
                minHumidityPct = 50.0,
                maxHumidityPct = 60.0,
                lockdownHumidityPct = 70.0,
                recommendedTurnsPerDay = 5,
                candlingDays = listOf(7, 14, 25),
                requiresTurning = true,
                tips = "Waterfowl require slightly higher humidity. After day 10, many breeders cool eggs for 10-15 minutes daily and lightly mist with warm water. Lockdown begins on day 25."
            ),
            SpeciesPreset(
                id = "quail",
                name = "Coturnix Quail",
                commonName = "Quail",
                incubationDays = 17,
                lockdownDay = 14,
                targetTempC = 37.6,
                minTempC = 37.3,
                maxTempC = 38.3,
                targetHumidityPct = 45.0,
                minHumidityPct = 40.0,
                maxHumidityPct = 52.0,
                lockdownHumidityPct = 65.0,
                recommendedTurnsPerDay = 6,
                candlingDays = listOf(6, 11, 14),
                requiresTurning = true,
                tips = "Fast incubation period of only 17 days. Quail eggshells are speckled and hard to candle; use a high-lumen cold light. Stop turning promptly on Day 14."
            ),
            SpeciesPreset(
                id = "goose",
                name = "Goose (Anser anser)",
                commonName = "Goose",
                incubationDays = 30,
                lockdownDay = 27,
                targetTempC = 37.4,
                minTempC = 37.0,
                maxTempC = 38.0,
                targetHumidityPct = 55.0,
                minHumidityPct = 50.0,
                maxHumidityPct = 60.0,
                lockdownHumidityPct = 75.0,
                recommendedTurnsPerDay = 4,
                candlingDays = listOf(8, 16, 27),
                requiresTurning = true,
                tips = "Goose eggs benefit greatly from 180° turns. Cool and mist eggs daily starting day 15 for 15 minutes. Increase humidity to 75% on day 27 lockdown."
            ),
            SpeciesPreset(
                id = "turkey",
                name = "Turkey (Meleagris gallopavo)",
                commonName = "Turkey",
                incubationDays = 28,
                lockdownDay = 25,
                targetTempC = 37.5,
                minTempC = 37.2,
                maxTempC = 38.3,
                targetHumidityPct = 50.0,
                minHumidityPct = 45.0,
                maxHumidityPct = 55.0,
                lockdownHumidityPct = 65.0,
                recommendedTurnsPerDay = 5,
                candlingDays = listOf(7, 14, 21),
                requiresTurning = true,
                tips = "Similar parameters to chicken, but with a 28-day cycle. Maintain stable temperature throughout. Lockdown begins on day 25."
            ),
            SpeciesPreset(
                id = "pheasant",
                name = "Pheasant",
                commonName = "Pheasant",
                incubationDays = 24,
                lockdownDay = 21,
                targetTempC = 37.5,
                minTempC = 37.2,
                maxTempC = 38.2,
                targetHumidityPct = 52.0,
                minHumidityPct = 48.0,
                maxHumidityPct = 58.0,
                lockdownHumidityPct = 68.0,
                recommendedTurnsPerDay = 5,
                candlingDays = listOf(7, 14, 21),
                requiresTurning = true,
                tips = "Delicate shell structure. Stop turning on day 21 and provide adequate ventilation without causing drafty temperature swings."
            ),
            SpeciesPreset(
                id = "reptile",
                name = "Reptile (Gecko / Tortoise)",
                commonName = "Reptile",
                incubationDays = 60,
                lockdownDay = 55,
                targetTempC = 28.5,
                minTempC = 26.5,
                maxTempC = 30.5,
                targetHumidityPct = 80.0,
                minHumidityPct = 75.0,
                maxHumidityPct = 88.0,
                lockdownHumidityPct = 80.0,
                recommendedTurnsPerDay = 0,
                candlingDays = listOf(14, 30),
                requiresTurning = false,
                tips = "IMPORTANT: Never rotate or turn reptile eggs! Turning will drown the developing embryo. Incubate in damp vermiculite or perlite (1:1 water to substrate ratio by weight)."
            ),
            SpeciesPreset(
                id = "custom",
                name = "Custom Species",
                commonName = "Custom",
                incubationDays = 21,
                lockdownDay = 18,
                targetTempC = 37.5,
                minTempC = 37.0,
                maxTempC = 38.5,
                targetHumidityPct = 50.0,
                minHumidityPct = 45.0,
                maxHumidityPct = 60.0,
                lockdownHumidityPct = 65.0,
                recommendedTurnsPerDay = 5,
                candlingDays = listOf(7, 14),
                requiresTurning = true,
                tips = "Consult reliable breeder guidelines and scientific husbandry manuals for your specific species. Recommended conditions vary by incubator design and altitude."
            )
        )

        fun getById(id: String): SpeciesPreset {
            return ALL.find { it.id.equals(id, ignoreCase = true) || it.commonName.equals(id, ignoreCase = true) }
                ?: ALL.first()
        }
    }
}
