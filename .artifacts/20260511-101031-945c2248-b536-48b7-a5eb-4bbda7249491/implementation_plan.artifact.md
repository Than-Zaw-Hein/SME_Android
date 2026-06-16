# SME Business Growth Pack: Intelligence, Profit & Parked Sales

This plan transforms the app into a high-end business tool by adding profit analytics, inventory alerts, and the ability to "Hold" sales for later.

## User Review Required

> [!IMPORTANT]
> **Data Migration**: Existing products will have `costPrice = 0` and `minStockLevel = 0`. Owners should update these to see accurate profit/alerts.
> **Parked Sales**: Held carts will be stored in a new `parked_sales` collection per shop. They do **not** reduce inventory until they are finalized.

## Proposed Changes

### 1. Data Model Enhancements

#### [ProductModel.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/data/model/ProductModel.kt)
- Add `costPrice: Double = 0.0`
- Add `minStockLevel: Int = 0`

#### [NEW] [ParkedSaleModel.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/data/model/ParkedSaleModel.kt)
- `id: String`, `customerName: String?`, `items: List<CartItem>`, `timestamp: Long`

### 2. POS Intelligence (Parked Sales)

#### [PosScreen.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/pos/PosScreen.kt)
- **Hold Button**: Add a "Pause/Hold" icon to the top bar or cart popup.
- **Resume Button**: Add a "Parked Sales" icon to the top bar (shows a badge with the count of held sales).
- **Selection**: A dialog to pick a parked sale to resume.

### 3. Inventory & Profit (UI/UX)

#### [StockManagementScreen.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/stock/StockManagementScreen.kt)
- **Low Stock UI**: Display a "LOW STOCK" badge in red when `qty <= minStockLevel`.

#### [HistoryScreen.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/ui/history/HistoryScreen.kt)
- **Profit Summary**: Show **Revenue**, **Total Cost**, and **Net Profit** (Calculated as `Total Sales - Total Costs`).

### 4. Logic & Repositories

#### [NEW] [ParkedSaleRepository.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/domain/repository/ParkedSaleRepository.kt)
- Functions to `holdSale`, `resumeSale`, and `getParkedSales`.

#### [TransactionRepositoryImpl.kt](file:///C:/Users/thanz/OneDrive/Desktop/Development/TZH/SME_Android/app/src/main/java/com/tzh/sme/data/repository/TransactionRepositoryImpl.kt)
- Snapshots the current `costPrice` of items during checkout to ensure historical profit accuracy even if prices change later.

## Verification Plan

### Manual Verification
1. **Hold/Resume Test**: Add items to cart -> Click Hold -> Clear Cart -> Open Parked Sales -> Click Resume. Verify cart is restored.
2. **Profit Test**: Sell a product with $5 cost for $15. Verify History shows $10 Net Profit.
3. **Alert Test**: Set "Min Stock" to 10. Change qty to 9. Verify Red alert appears in Stock list.
