//package com.cardify.app.ui.shared_info
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.cardify.app.data.model.ShareItem
//import com.cardify.app.ui.components.AppScaffold
//import com.cardify.app.ui.home.CardifyColors
//
//@Composable
//fun SharedInfoScreen(
//    onNavigate: (String) -> Unit,
//    viewModel: SharedInfoViewModel = viewModel()
//) {
//    val shares by viewModel.filteredShares.collectAsState()
//    val filterState by viewModel.filterState.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//
//    LaunchedEffect(Unit) {
//        viewModel.refreshShares()
//    }
//
//    AppScaffold(currentRoute = "wallet", onNavigate = onNavigate) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.White)
//                .padding(padding)
//        ) {
//            // בחירת כיוון השיתוף (נכנס/יוצא)
//            DirectionSelector(
//                selectedDirection = filterState.direction,
//                onDirectionSelected = { viewModel.setDirection(it) }
//            )
//
//            if (isLoading) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator(color = CardifyColors.DarkGreen)
//                }
//            } else if (shares.isEmpty()) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Text("No transactions found", color = Color.Gray)
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                    contentPadding = PaddingValues(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    items(shares) { share ->
//                        SharedTransactionItem(share = share)
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun SharedTransactionItem(share: ShareItem) {
//    val txn = share.transaction
//
//    // התאמה למודל שלך: משתמשים ב-direction במקום ב-isOutgoing
//    val isOutgoing = share.direction == "outgoing"
//    // מכיוון שאין friendName במודל ששלחת, נציג את הטלפון של הצד השני
//    val displayContact = if (isOutgoing) share.sharedWith else share.sharedBy
//
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
//        shape = RoundedCornerShape(12.dp)
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(
//                text = if (isOutgoing) "To: $displayContact" else "From: $displayContact",
//                fontWeight = FontWeight.Bold,
//                color = CardifyColors.DarkGreen
//            )
//
//            if (txn != null) {
//                Spacer(modifier = Modifier.height(8.dp))
//                HorizontalDivider(color = Color(0xFFEEEEEE))
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                    Text(txn.businessName, fontWeight = FontWeight.Bold)
//                    Text("₪${txn.amount}")
//                }
//                Text(txn.category, fontSize = 12.sp, color = Color.Gray)
//                Text(txn.date, fontSize = 11.sp, color = Color.LightGray)
//            }
//        }
//    }
//}
//
//@Composable
//fun DirectionSelector(selectedDirection: String, onDirectionSelected: (String) -> Unit) {
//    Row(
//        modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
//    ) {
//        listOf("outgoing" to "Sent", "incoming" to "Received").forEach { (key, label) ->
//            val isSelected = selectedDirection == key
//            Box(
//                modifier = Modifier
//                    .weight(1f)
//                    .clip(RoundedCornerShape(12.dp))
//                    .background(if (isSelected) CardifyColors.DarkGreen else Color.Transparent)
//                    .clickable { onDirectionSelected(key) }
//                    .padding(vertical = 12.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(label, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
//            }
//        }
//    }
//}