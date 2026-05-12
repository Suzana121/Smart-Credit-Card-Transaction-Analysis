package com.cardify.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardify.app.R
import com.cardify.app.data.model.Friend
import com.cardify.app.data.model.Transaction

enum class TransactionRowVariant { FULL, SHARED, COMPACT }

data class TransactionItem(
    val id: String,
    val title: String,
    val date: String,
    val amount: Double,
    val isIrregular: Boolean = false,
    val category: String = "Other",
    val sharedWith: Friend? = null
)

/** Converts a backend [Transaction] to the UI model [TransactionItem]. */
fun Transaction.toTransactionItem(sharedWith: Friend? = null) = TransactionItem(
    id          = id,
    title       = businessName,
    date        = date,
    amount      = amount,
    isIrregular = status == "IRREGULAR",
    category    = category,
    sharedWith  = sharedWith
)

fun getCategoryIcon(category: String): Int = when {
    category.contains("Food", ignoreCase = true) ||
            category.contains("Grocery", ignoreCase = true) ||
            category.contains("מזון", ignoreCase = true)        -> R.drawable.food1
    category.contains("Health", ignoreCase = true) ||
            category.contains("בריאות", ignoreCase = true)      -> R.drawable.health
    category.contains("Shopping", ignoreCase = true) ||
            category.contains("Fashion", ignoreCase = true) ||
            category.contains("אופנה", ignoreCase = true) ||
            category.contains("קניות", ignoreCase = true)       -> R.drawable.shopping1
    category.contains("Transport", ignoreCase = true) ||
            category.contains("תחבורה", ignoreCase = true)      -> R.drawable.transport
    category.contains("Education", ignoreCase = true) ||
            category.contains("חינוך", ignoreCase = true)       -> R.drawable.education
    else                                                        -> R.drawable.other
}

@Composable
fun TransactionRow(
    transaction:          TransactionItem,
    variant:              TransactionRowVariant = TransactionRowVariant.FULL,
    onShareClick:         () -> Unit = {},
    onStatusChange:       (Boolean) -> Unit = {},
    // ── תוכן נוסף אופציונלי שיוצג בתוך הפרטים המורחבים (לפני הכפתורים) ──
    extraExpandedContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val iconSize: Dp         = if (variant == TransactionRowVariant.FULL) 24.dp else 18.dp
    val titleSize: TextUnit  = if (variant == TransactionRowVariant.FULL) 15.sp else 13.sp
    val amountSize: TextUnit = if (variant == TransactionRowVariant.FULL) 14.sp else 12.sp
    val statusSize: TextUnit = if (variant == TransactionRowVariant.FULL) 12.sp else 10.sp
    val padH: Dp             = if (variant == TransactionRowVariant.FULL) 16.dp else 12.dp
    val padV: Dp             = if (variant == TransactionRowVariant.FULL) 14.dp else 10.dp

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (variant == TransactionRowVariant.SHARED) {
            Image(
                painter            = painterResource(id = transaction.sharedWith?.photoResource ?: R.drawable.user),
                contentDescription = transaction.sharedWith?.name,
                modifier           = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .clickable { isExpanded = !isExpanded },
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                1.dp,
                if (transaction.isIrregular) Color(0xFFE23125) else Color(0xFFEEEEEE)
            )
        ) {
            Column {
                Row(
                    modifier          = Modifier
                        .padding(horizontal = padH, vertical = padV)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter            = painterResource(id = getCategoryIcon(transaction.category)),
                        contentDescription = transaction.category,
                        modifier           = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = transaction.title,
                        fontSize   = titleSize,
                        fontWeight = FontWeight.Bold,
                        color      = Color.Black,
                        modifier   = Modifier.weight(1f)
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(-9.dp)
                    ) {
                        Text(
                            text       = "₪${transaction.amount.toInt()}",
                            fontSize   = amountSize,
                            fontWeight = FontWeight.Bold,
                            color      = Color.Black
                        )
                        Text(
                            text       = if (transaction.isIrregular) "Irregular" else "Regular",
                            fontSize   = statusSize,
                            fontWeight = FontWeight.Bold,
                            color      = if (transaction.isIrregular) Color(0xFFE23125) else Color(0xFF38D325)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut()
                ) {
                    ExpandedTransactionDetails(
                        transaction          = transaction,
                        padH                 = padH,
                        padV                 = padV,
                        onShareClick         = onShareClick,
                        onStatusChange       = onStatusChange,
                        extraExpandedContent = extraExpandedContent
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandedTransactionDetails(
    transaction:          TransactionItem,
    padH:                 Dp,
    padV:                 Dp,
    onShareClick:         () -> Unit,
    onStatusChange:       (Boolean) -> Unit,
    extraExpandedContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = padH)
            .padding(bottom = padV)
    ) {
        HorizontalDivider(color = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Date Of Transaction: ${transaction.date}",
            fontSize   = 12.sp,
            color      = Color.Black,
            fontWeight = FontWeight.SemiBold
        )

        if (transaction.isIrregular) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please Notice: Unrecognizable Transaction!",
                color      = Color(0xFFE23125),
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.fillMaxWidth(),
                textAlign  = TextAlign.Center
            )
        }

        // ── תוכן נוסף (קטגוריה + הסבר) שמגיע מעמוד הטרנזקציות ──
        if (extraExpandedContent != null) {
            Spacer(modifier = Modifier.height(8.dp))
            extraExpandedContent()
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8FCE8))
                    .clickable { onShareClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint     = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick        = { onStatusChange(!transaction.isIrregular) },
                modifier       = Modifier.height(38.dp).widthIn(min = 170.dp),
                shape          = RoundedCornerShape(12.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF006769)),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector        = if (transaction.isIrregular) Icons.Default.CheckBox else Icons.Default.Warning,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text     = if (transaction.isIrregular) "Mark as \"Regular\"" else "Report as \"Irregular\"",
                    fontSize = 11.sp
                )
            }
        }
    }
}