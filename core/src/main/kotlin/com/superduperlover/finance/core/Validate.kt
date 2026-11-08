package com.superduperlover.finance.core

/** Returns Vietnamese error messages for [txn]; an empty list means the transaction is valid. */
fun validateTxn(txn: Txn, accounts: List<Account>, categories: List<Category>): List<String> {
    val errors = mutableListOf<String>()

    if (txn.amount <= 0) errors += "Số tiền phải lớn hơn 0"

    val account = accounts.firstOrNull { it.id == txn.accountId }
    if (account == null) errors += "Chưa chọn tài khoản"
    else if (account.archived) errors += "Tài khoản đã được lưu trữ"

    when (txn.kind) {
        TxnKind.TRANSFER -> {
            if (txn.toAccountId == null) {
                errors += "Chưa chọn tài khoản nhận"
            } else if (txn.toAccountId == txn.accountId) {
                errors += "Tài khoản nhận phải khác tài khoản gửi"
            } else {
                val to = accounts.firstOrNull { it.id == txn.toAccountId }
                if (to == null) errors += "Tài khoản nhận không tồn tại"
                else if (to.archived) errors += "Tài khoản nhận đã được lưu trữ"
            }
            if (txn.categoryId != null) errors += "Chuyển khoản không có danh mục"
        }
        TxnKind.EXPENSE, TxnKind.INCOME -> {
            val wanted = if (txn.kind == TxnKind.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
            if (txn.categoryId == null) {
                errors += "Chưa chọn danh mục"
            } else {
                val category = categories.firstOrNull { it.id == txn.categoryId }
                if (category == null) errors += "Danh mục không tồn tại"
                else {
                    if (category.kind != wanted) errors += "Danh mục không khớp loại giao dịch"
                    if (category.archived) errors += "Danh mục đã được lưu trữ"
                }
            }
        }
    }
    return errors
}
