package com.superduperlover.finance.core

import java.time.LocalDate

/**
 * Returns Vietnamese error messages for [txn]; an empty list means the transaction is valid.
 * [original] = the stored version when [txn] is an edit: an archived account or category is accepted when the
 * stored transaction already uses it, so old transactions stay editable.
 */
fun validateTxn(
    txn: Txn,
    accounts: List<Account>,
    categories: List<Category>,
    original: Txn? = null,
    today: LocalDate = LocalDate.now(),
): List<String> {
    val errors = mutableListOf<String>()

    if (txn.amount <= 0) errors += "Số tiền phải lớn hơn 0"
    if (txn.date.isAfter(today) && original?.date != txn.date) errors += "Ngày không được sau hôm nay"

    val account = accounts.firstOrNull { it.id == txn.accountId }
    if (account == null) errors += "Chưa chọn tài khoản"
    else if (account.archived && original?.accountId != account.id) errors += "Tài khoản đã được lưu trữ"

    when (txn.kind) {
        TxnKind.TRANSFER -> {
            if (txn.toAccountId == null) {
                errors += "Chưa chọn tài khoản nhận"
            } else if (txn.toAccountId == txn.accountId) {
                errors += "Tài khoản nhận phải khác tài khoản gửi"
            } else {
                val to = accounts.firstOrNull { it.id == txn.toAccountId }
                if (to == null) errors += "Tài khoản nhận không tồn tại"
                else if (to.archived && original?.toAccountId != to.id) errors += "Tài khoản nhận đã được lưu trữ"
                if (to != null && account != null) {
                    if (to.currency != account.currency) {
                        // different currencies: both amounts are recorded
                        if (txn.toAmount == null || txn.toAmount <= 0) errors += "Nhập số tiền nhận (khác loại tiền)"
                    } else if (txn.toAmount != null && txn.toAmount != txn.amount) {
                        errors += "Số tiền nhận phải bằng số tiền gửi (cùng loại tiền)"
                    }
                }
            }
            if (txn.categoryId != null) errors += "Chuyển khoản không có danh mục"
        }
        TxnKind.EXPENSE, TxnKind.INCOME -> {
            if (txn.toAmount != null) errors += "Số tiền nhận chỉ dùng cho chuyển khoản"
            val wanted = if (txn.kind == TxnKind.EXPENSE) CategoryKind.EXPENSE else CategoryKind.INCOME
            if (txn.categoryId == null) {
                errors += "Chưa chọn danh mục"
            } else {
                val category = categories.firstOrNull { it.id == txn.categoryId }
                if (category == null) errors += "Danh mục không tồn tại"
                else {
                    if (category.kind != wanted) errors += "Danh mục không khớp loại giao dịch"
                    if (category.archived && original?.categoryId != category.id) errors += "Danh mục đã được lưu trữ"
                }
            }
        }
    }
    return errors
}
