package moe.nanakura.megumi.trakteer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrakteerResponse<T>(
        val status: String,
        @SerialName("status_code") val statusCode: Int,
        val result: T,
        val message: String
)

@Serializable data class TrakteerSupportsResult(val data: List<TrakteerSupport>)

@Serializable
data class TrakteerSupport(
        @SerialName("supporter_name") val supporterName: String? = null,
        @SerialName("support_message") val supportMessage: String? = null,
        val quantity: Int? = null,
        val amount: Int? = null,
        @SerialName("unit_name") val unitName: String? = null,
        val status: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
        @SerialName("payment_method") val paymentMethod: String? = null,
        @SerialName("order_id") val orderId: String? = null,
        @SerialName("supporter_email") val supporterEmail: String? = null,
        @SerialName("is_guest") val isGuest: Boolean? = null,
        @SerialName("reply_message") val replyMessage: String? = null,
        @SerialName("net_amount") val netAmount: Int? = null,
        @SerialName("updated_at_diff_label") val updatedAtDiffLabel: String? = null
)
