# Professional Receipt Preview

This is a visual mock-up of what your new printed receipt looks like on standard 58mm thermal paper.

---
```text
          UNIQUE STORE          <- [C] Bold, Uppercase
       123 Business Road        <- [C] Shop Address
         Tel: 091234567         <- [C] Phone Number
-------------------------------- <- [C] Separator

Date: 14-05-2026 16:45:12        <- [L] Current Time
Cashier: Kyaw Kyaw              <- [L] Active User
Order ID: JZMIVC89              <- [L] Last 8 of ID
-------------------------------- <- [C] Separator

Ramen Noodles                   <- [L] Product Name
  1 x $5.00              $5.00  <- [L] Qty x Price [R] Total

Iced Coffee                     <- [L] Product Name
  2 x $3.50              $7.00  <- [L] Qty x Price [R] Total

-------------------------------- <- [C] Separator
TOTAL                   $12.00  <- [L] Label [R] Amount
DISCOUNT                 $1.00  <- [L] Label [R] Amount
NET TOTAL               $11.00  <- [L] Bold [R] Bold Amount
-------------------------------- <- [C] Separator

    Thank you for your visit!   <- [C] Footer Message

         [ QR CODE ]            <- [C] Scannable ID


                                <- [Extra Line Feeds for Tearing]
```
---

### Key Features shown in Preview:
*   **Hierarchical Header**: Your business name is emphasized.
*   **Staff Tracking**: The "Cashier" name helps with internal audits.
*   **Itemized Details**: Clear breakdown of quantity, unit price, and subtotal.
*   **Bold Totals**: The Net Total stands out for the customer.
*   **Verification**: The QR code at the bottom makes the receipt scannable for your future "Returns" or "Verify" feature.
