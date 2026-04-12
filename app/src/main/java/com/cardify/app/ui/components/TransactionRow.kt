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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.People
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

/**
 * Controls the visual density and layout of a [TransactionRow].
 *
 * - [FULL]    — full-size card with icons, amounts, and an expandable action panel.
 * - [SHARED]  — same as [FULL] but prepends the friend's avatar on the left.
 * - [COMPACT] — reduced padding and font sizes for use inside tighter layouts.
 */
enum class TransactionRowVariant { FULL, SHARED, COMPACT }

/**
 * UI model representing a single transaction row in the transaction list.
 *
 * @property id Unique transaction identifier used as a list key.
 * @property title Business or merchant name displayed as the row heading.
 * @property date Human-readable transaction date string.
 * @property amount Transaction amount in the local currency.
 * @property isIrregular `true` when the transaction is flagged as suspicious/irregular.
 * @property category Spending category name used to select the row icon.
 * @property sharedWith The [Friend] this transaction was shared with, or `null` when not shared.
 */
data class TransactionItem(
    val id: String,
    val title: String,
    val date: String,
    val amount: Double,
    val isIrregular: Boolean = false,
    val category: String = "Other",
    val sharedWith: Friend? = null
)

/**
 * Returns the drawable resource ID for the icon that represents [category].
 *
 * @param category The spending category name (case-insensitive).
 * @return A drawable resource ID from the app's icon set, falling back to `R.drawable.other`.
 */
fun getCategoryIcon(category: String): Int = when (category.lowercase()) {
    "food"      -> R.drawable.food1
    "health"    -> R.drawable.health
    "shopping"  -> R.drawable.shopping1
    "transport" -> R.drawable.transport
    "education" -> R.drawable.education
    else        -> R.drawable.other
}

/**
 * Confirmation dialog shown before toggling a transaction's Regular/Irregular status.
 *
 * @param isCurrentlyIrregular `true` when the transaction is currently irregular; determines
 *   the dialog message and confirm-button colour.
 * @param onConfirm Called when the user taps the confirm button.
 * @param onDismiss Called when the user taps Cancel or dismisses the dialog.
 */
@Composable
fun ConfirmStatusChangeDialog(
    isCurrentlyIrregular: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Are you sure?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                if (isCurrentlyIrregular)
                    "Mark this transaction as Regular?"
                else
                    "Mark this transaction as suspicious (Irregular)?",
                fontSize = 14.sp,
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyIrregular) Color(0xFF2E7D32)
                    else Color(0xFFE23125)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Yes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Modal bottom sheet that displays the user's friend list for sharing a transaction.
 *
 * When [friends] is empty it shows an empty-state illustration with a hint to visit the
 * Account screen. Otherwise it renders a scrollable list of friend rows each with a
 * "Send" button. The button is disabled while [isSending] is `true`.
 *
 * @param friends The list of friends to display.
 * @param isSending `true` while a share request is in flight (disables all Send buttons).
 * @param onSend Called with the selected [Friend] when a Send button is tapped.
 * @param onDismiss Called when the sheet is dismissed without selecting a friend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    friends: List<Friend>,
    isSending: Boolean,
    onSend: (Friend) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Share with Friends",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (friends.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.People,
                        null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "You haven't added any friends yet",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Go to the Account screen to add friends",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = friend.photoResource),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                friend.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { onSend(friend) },
                                enabled = !isSending,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006769)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Send", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Primary reusable transaction card composable used in the home and transactions screens.
 *
 * Renders the transaction's category icon, merchant name, amount, and
 * Regular/Irregular status. Tapping the card expands an [ExpandedTransactionDetails] panel
 * with the date, a Share button, and a status-toggle button. The status toggle shows a
 * [ConfirmStatusChangeDialog] before committing the change. Sharing opens a [ShareBottomSheet].
 *
 * Local `isIrregular` state is keyed on [TransactionItem.id] so it resets correctly when
 * the list is recomposed with different items.
 *
 * @param transaction The [TransactionItem] data to display.
 * @param variant Controls the visual density and optional friend-avatar prefix.
 * @param friends Friend list forwarded to [ShareBottomSheet].
 * @param isSendingShare `true` while a share request is in flight.
 * @param onSendShare Called with the selected [Friend] when the user confirms a share.
 * @param onStatusChange Called with the new `isIrregular` value after the user confirms a status change.
 */
@Composable
fun TransactionRow(
    transaction: TransactionItem,
    variant: TransactionRowVariant = TransactionRowVariant.FULL,
    friends: List<Friend> = emptyList(),
    isSendingShare: Boolean = false,
    onSendShare: (Friend) -> Unit = {},
    onStatusChange: (Boolean) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    // סטייט מקומי שעוקב אחרי הסטטוס - מתאפס כשה-id משתנה
    var isIrregular by remember(transaction.id) { mutableStateOf(transaction.isIrregular) }

    val iconSize: Dp         = if (variant == TransactionRowVariant.FULL) 24.dp else 18.dp
    val titleSize: TextUnit  = if (variant == TransactionRowVariant.FULL) 15.sp else 13.sp
    val amountSize: TextUnit = if (variant == TransactionRowVariant.FULL) 14.sp else 12.sp
    val statusSize: TextUnit = if (variant == TransactionRowVariant.FULL) 12.sp else 10.sp
    val padH: Dp             = if (variant == TransactionRowVariant.FULL) 16.dp else 12.dp
    val padV: Dp             = if (variant == TransactionRowVariant.FULL) 14.dp else 10.dp

    // --- Dialog אישור ---
    if (showConfirmDialog) {
        ConfirmStatusChangeDialog(
            isCurrentlyIrregular = isIrregular,
            onConfirm = {
                showConfirmDialog = false
                isIrregular = !isIrregular       // עדכון מיידי של ה-UI
                onStatusChange(isIrregular)       // שליחה ל-ViewModel
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    // --- Bottom Sheet שיתוף ---
    if (showShareSheet) {
        ShareBottomSheet(
            friends = friends,
            isSending = isSendingShare,
            onSend = { friend ->
                onSendShare(friend)
                showShareSheet = false
            },
            onDismiss = { showShareSheet = false }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (variant == TransactionRowVariant.SHARED) {
            Image(
                painter = painterResource(
                    id = transaction.sharedWith?.photoResource ?: R.drawable.user
                ),
                contentDescription = transaction.sharedWith?.name,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(
                1.dp,
                if (isIrregular) Color(0xFFE23125) else Color(0xFFEEEEEE)
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = padH, vertical = padV)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = getCategoryIcon(transaction.category)),
                        contentDescription = transaction.category,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = transaction.title,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(-9.dp)
                    ) {
                        Text(
                            text = "₪${transaction.amount.toInt()}",
                            fontSize = amountSize,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        // הכיתוב והצבע מתעדכנים לפי isIrregular המקומי
                        Text(
                            text = if (isIrregular) "Irregular" else "Regular",
                            fontSize = statusSize,
                            fontWeight = FontWeight.Bold,
                            color = if (isIrregular) Color(0xFFE23125) else Color(0xFF38D325)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ExpandedTransactionDetails(
                        transaction = transaction.copy(isIrregular = isIrregular),
                        padH = padH,
                        padV = padV,
                        onShareClick = { showShareSheet = true },
                        onStatusChange = { showConfirmDialog = true }
                    )
                }
            }
        }
    }
}

/**
 * Animated expanded section shown below the main transaction row when the card is tapped.
 *
 * Displays the transaction date and, when the transaction is irregular, a prominent warning
 * message. Also renders a Share button and a status-toggle button.
 *
 * @param transaction The [TransactionItem] whose expanded details are shown.
 * @param padH Horizontal padding matching the parent row's padding.
 * @param padV Vertical padding matching the parent row's padding.
 * @param onShareClick Called when the Share button is tapped (opens [ShareBottomSheet]).
 * @param onStatusChange Called when the status-toggle button is tapped (opens [ConfirmStatusChangeDialog]).
 */
@Composable
fun ExpandedTransactionDetails(
    transaction: TransactionItem,
    padH: Dp,
    padV: Dp,
    onShareClick: () -> Unit,
    onStatusChange: () -> Unit
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
            fontSize = 12.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold
        )

        if (transaction.isIrregular) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please Notice: Unrecognizable Transaction!",
                color = Color(0xFFE23125),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // כפתור Share
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8FCE8))
                    .clickable { onShareClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Share, null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Share", fontSize = 12.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // כפתור שינוי סטטוס - פותח Dialog
            Button(
                onClick = onStatusChange,
                modifier = Modifier
                    .height(38.dp)
                    .widthIn(min = 170.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transaction.isIrregular) Color(0xFF2E7D32)
                    else Color(0xFF006769)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (transaction.isIrregular) Icons.Default.CheckBox
                    else Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (transaction.isIrregular) "Mark as \"Regular\""
                    else "Report as \"Irregular\"",
                    fontSize = 11.sp
                )
            }
        }
    }
}