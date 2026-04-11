package com.example.assignment3_cos30017.data.repository

import com.example.assignment3_cos30017.data.model.Car
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CarFilterSortTest {

    private lateinit var cars: List<Car>

    @Before
    fun setUp() {
        cars = listOf(
            Car(id = "1", title = "BMW M3", model = "Sedan", year = 2023,
                dailyCost = 150, status = Car.STATUS_LISTED, createdAt = 1000),
            Car(id = "2", title = "Toyota Camry", model = "Sedan", year = 2020,
                dailyCost = 80, status = Car.STATUS_UNLISTED, createdAt = 2000),
            Car(id = "3", title = "Tesla Model 3", model = "Electric", year = 2024,
                dailyCost = 200, status = Car.STATUS_RENTED, createdAt = 3000),
            Car(id = "4", title = "Honda Civic", model = "Hatchback", year = 2022,
                dailyCost = 60, status = Car.STATUS_LISTED, createdAt = 4000),
            Car(id = "5", title = "Porsche 911", model = "Supercar", year = 2025,
                dailyCost = 300, status = Car.STATUS_UNLISTED, createdAt = 5000)
        )
    }

    // --- filterCars ---

    @Test
    fun filter_all_returns_all_cars() {
        val result = CarRepository.filterCars(cars, CarRepository.FilterMode.ALL)
        assertEquals(5, result.size)
    }

    @Test
    fun filter_listed_returns_only_listed() {
        val result = CarRepository.filterCars(cars, CarRepository.FilterMode.LISTED)
        assertEquals(2, result.size)
        assertTrue(result.all { it.effectiveStatus == Car.STATUS_LISTED })
    }

    @Test
    fun filter_unlisted_returns_only_unlisted() {
        val result = CarRepository.filterCars(cars, CarRepository.FilterMode.UNLISTED)
        assertEquals(2, result.size)
        assertTrue(result.all { it.effectiveStatus == Car.STATUS_UNLISTED })
    }

    @Test
    fun filter_rented_returns_only_rented() {
        val result = CarRepository.filterCars(cars, CarRepository.FilterMode.RENTED)
        assertEquals(1, result.size)
        assertEquals("3", result[0].id)
    }

    @Test
    fun filter_on_empty_list_returns_empty() {
        val result = CarRepository.filterCars(emptyList(), CarRepository.FilterMode.LISTED)
        assertTrue(result.isEmpty())
    }

    @Test
    fun filter_uses_effectiveStatus_for_legacy_cars() {
        val legacyCars = listOf(
            Car(id = "legacy1", status = "", isAvailable = true),
            Car(id = "legacy2", status = "", isAvailable = false)
        )
        val listed = CarRepository.filterCars(legacyCars, CarRepository.FilterMode.LISTED)
        assertEquals(1, listed.size)
        assertEquals("legacy1", listed[0].id)

        val rented = CarRepository.filterCars(legacyCars, CarRepository.FilterMode.RENTED)
        assertEquals(1, rented.size)
        assertEquals("legacy2", rented[0].id)
    }

    // --- sortCars ---

    @Test
    fun sort_by_year_descending() {
        val result = CarRepository.sortCars(cars, CarRepository.SortMode.YEAR_DESC)
        assertEquals(listOf(2025, 2024, 2023, 2022, 2020), result.map { it.year })
    }

    @Test
    fun sort_by_cost_ascending() {
        val result = CarRepository.sortCars(cars, CarRepository.SortMode.COST_ASC)
        assertEquals(listOf(60, 80, 150, 200, 300), result.map { it.dailyCost })
    }

    @Test
    fun sort_by_newest_uses_createdAt_descending() {
        val result = CarRepository.sortCars(cars, CarRepository.SortMode.NEWEST)
        assertEquals(listOf(5000L, 4000L, 3000L, 2000L, 1000L), result.map { it.createdAt })
    }

    @Test
    fun sort_empty_list_returns_empty() {
        val result = CarRepository.sortCars(emptyList(), CarRepository.SortMode.NEWEST)
        assertTrue(result.isEmpty())
    }

    @Test
    fun sort_single_item_returns_same() {
        val single = listOf(Car(id = "only"))
        val result = CarRepository.sortCars(single, CarRepository.SortMode.YEAR_DESC)
        assertEquals(1, result.size)
        assertEquals("only", result[0].id)
    }

    @Test
    fun sort_preserves_all_elements() {
        val result = CarRepository.sortCars(cars, CarRepository.SortMode.COST_ASC)
        assertEquals(cars.size, result.size)
        assertEquals(cars.map { it.id }.toSet(), result.map { it.id }.toSet())
    }

    // --- filter + sort combined ---

    @Test
    fun filter_then_sort_works_correctly() {
        val listed = CarRepository.filterCars(cars, CarRepository.FilterMode.LISTED)
        val sorted = CarRepository.sortCars(listed, CarRepository.SortMode.COST_ASC)
        assertEquals(2, sorted.size)
        assertEquals("Honda Civic", sorted[0].title) // cost 60
        assertEquals("BMW M3", sorted[1].title)      // cost 150
    }

    @Test
    fun sort_by_cost_with_same_cost_is_stable() {
        val sameCost = listOf(
            Car(id = "a", dailyCost = 100, createdAt = 1),
            Car(id = "b", dailyCost = 100, createdAt = 2)
        )
        val result = CarRepository.sortCars(sameCost, CarRepository.SortMode.COST_ASC)
        assertEquals(2, result.size)
        // sortedBy is stable, so original order is preserved
        assertEquals("a", result[0].id)
        assertEquals("b", result[1].id)
    }
}
