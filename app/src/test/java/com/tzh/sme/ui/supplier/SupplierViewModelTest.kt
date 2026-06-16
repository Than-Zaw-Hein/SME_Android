package com.tzh.sme.ui.supplier

import app.cash.turbine.test
import com.tzh.sme.data.model.SupplierModel
import com.tzh.sme.domain.usecase.common.GetProductsUseCase
import com.tzh.sme.domain.usecase.supplier.*
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val getSuppliersUseCase = mockk<GetSuppliersUseCase>()
    private val getSupplierByIdUseCase = mockk<GetSupplierByIdUseCase>()
    private val addSupplierUseCase = mockk<AddSupplierUseCase>()
    private val updateSupplierUseCase = mockk<UpdateSupplierUseCase>()
    private val deleteSupplierUseCase = mockk<DeleteSupplierUseCase>()
    private val filterSuppliersUseCase = mockk<FilterSuppliersUseCase>()
    private val getProductsUseCase = mockk<GetProductsUseCase>()

    private lateinit var useCases: SupplierUseCases
    private lateinit var viewModel: SupplierViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        useCases = SupplierUseCases(
            getSuppliers = getSuppliersUseCase,
            getSupplierById = getSupplierByIdUseCase,
            addSupplier = addSupplierUseCase,
            updateSupplier = updateSupplierUseCase,
            deleteSupplier = deleteSupplierUseCase,
            filterSuppliers = filterSuppliersUseCase,
            getProducts = getProductsUseCase
        )

        coEvery { getSuppliersUseCase() } returns flowOf(emptyList())
        every { filterSuppliersUseCase(any(), any()) } returns emptyList()

        viewModel = SupplierViewModel(useCases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveSupplier with empty name should show error`() = runTest {
        viewModel.sendIntent(SupplierUiIntent.SaveSupplier("", "", "", ""))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Supplier name cannot be empty", state.error)
        }
    }

    @Test
    fun `saveSupplier with valid data should call addSupplier and navigate back`() = runTest {
        val name = "New Supplier"
        val contact = "123456"
        val address = "Yangon"
        val category = "Electronics"

        coEvery { addSupplierUseCase(any()) } returns Result.success("new_id")
        viewModel.sendIntent(SupplierUiIntent.SaveSupplier(name, contact, address, category))

        coVerify { addSupplierUseCase(match { it.name == name && it.contact == contact }) }
        
        viewModel.sideEffect.test {
            assertEquals(SupplierUiSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `deleteSupplier should call deleteSupplier and navigate back`() = runTest {
        val id = "supplier123"
        coEvery { deleteSupplierUseCase(id) } returns Result.success(Unit)

        viewModel.sendIntent(SupplierUiIntent.DeleteSupplier(id))

        coVerify { deleteSupplierUseCase(id) }
        viewModel.sideEffect.test {
            assertEquals(SupplierUiSideEffect.NavigateBack, awaitItem())
        }
    }
}
